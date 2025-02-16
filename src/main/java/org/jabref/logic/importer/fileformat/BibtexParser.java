package org.jabref.logic.importer.fileformat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SaveConfiguration;
import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.jabref.logic.util.MetadataSerializationConfiguration.GROUP_QUOTE_CHAR;
import static org.jabref.logic.util.MetadataSerializationConfiguration.GROUP_TYPE_SUFFIX;

/**
 * Class for importing BibTeX-files.
 * <p>
 * Use:
 * <p>
 * <code>BibtexParser parser = new BibtexParser(reader);</code>
 * <p>
 * <code>ParserResult result = parser.parse();</code>
 * <p>
 * or
 * <p>
 * <code>ParserResult result = BibtexParser.parse(reader);</code>
 * <p>
 * Can be used stand-alone.
 * <p>
 * Main using method: {@link org.jabref.logic.importer.OpenDatabase#loadDatabase(java.nio.file.Path, org.jabref.logic.importer.ImportFormatPreferences, org.jabref.model.util.FileUpdateMonitor)}
 * <p>
 * Opposite class: {@link org.jabref.logic.exporter.BibDatabaseWriter}
 */
public class BibtexParser implements Parser {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexParser.class);
    private static final Integer LOOKAHEAD = 1024;
    private static final String BIB_DESK_ROOT_GROUP_NAME = "BibDeskGroups";
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private final Deque<Character> pureTextFromFile = new LinkedList<>();
    private final ImportFormatPreferences importFormatPreferences;
    private PushbackReader pushbackReader;
    private BibDatabase database;
    private Set<BibEntryType> entryTypes;
    private boolean eof;
    private int line = 1;
    private ParserResult parserResult;
    private final MetaDataParser metaDataParser;
    private final Map<String, String> parsedBibdeskGroups;

    private GroupTreeNode bibDeskGroupTreeNode;

    public BibtexParser(ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor) {
        this.importFormatPreferences = Objects.requireNonNull(importFormatPreferences);
        this.metaDataParser = new MetaDataParser(fileMonitor);
        this.parsedBibdeskGroups = new HashMap<>();
    }

    public BibtexParser(ImportFormatPreferences importFormatPreferences) {
        this(importFormatPreferences, new DummyFileUpdateMonitor());
    }

    /**
     * Parses BibtexEntries from the given string and returns one entry found (or null if none found)
     * <p>
     * It is undetermined which entry is returned, so use this in case you know there is only one entry in the string.
     *
     * @return An {@code Optional<BibEntry>. Optional.empty()} if non was found or an error occurred.
     */
    public static Optional<BibEntry> singleFromString(String bibtexString, ImportFormatPreferences importFormatPreferences) throws ParseException {
        Collection<BibEntry> entries = new BibtexParser(importFormatPreferences).parseEntries(bibtexString);
        if ((entries == null) || entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(entries.iterator().next());
    }

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        Reader reader;
        try {
            reader = Importer.getReader(inputStream);
            return parse(reader).getDatabase().getEntries();
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    public Collection<BibtexString> getStringValues() {
        return database.getStringValues();
    }

    public Optional<BibEntry> parseSingleEntry(String bibtexString) throws ParseException {
        return parseEntries(bibtexString).stream().findFirst();
    }

    /**
     * Parses BibTeX data found when reading from reader.
     * <p>
     * The reader will be consumed.
     * <p>
     * Multiple calls to parse() return the same results
     * <p>
     * Handling of encoding is done at {@link BibtexImporter}
     */
    public ParserResult parse(Reader in) throws IOException {
        Objects.requireNonNull(in);
        pushbackReader = new PushbackReader(in, BibtexParser.LOOKAHEAD);

        String newLineSeparator = determineNewLineSeparator();

        // BibTeX related contents
        initializeParserResult(newLineSeparator);

        parseDatabaseID();

        skipWhitespace();

        return parseFileContent();
    }

    private String determineNewLineSeparator() throws IOException {
        String newLineSeparator = OS.NEWLINE;
        StringWriter stringWriter = new StringWriter(BibtexParser.LOOKAHEAD);
        int i = 0;
        int currentChar;
        do {
            currentChar = pushbackReader.read();
            stringWriter.append((char) currentChar);
            i++;
        } while ((i < BibtexParser.LOOKAHEAD) && (currentChar != '\r') && (currentChar != '\n'));
        if (currentChar == '\r') {
            newLineSeparator = "\r\n";
        } else if (currentChar == '\n') {
            newLineSeparator = "\n";
        }

        // unread all sneaked characters
        pushbackReader.unread(stringWriter.toString().toCharArray());

        return newLineSeparator;
    }

    private void initializeParserResult(String newLineSeparator) {
        database = new BibDatabase();
        database.setNewLineSeparator(newLineSeparator);
        entryTypes = new HashSet<>(); // To store custom entry types parsed.
        parserResult = new ParserResult(database, new MetaData(), entryTypes);
    }

    private void parseDatabaseID() throws IOException {
        while (!eof) {
            skipWhitespace();
            char c = (char) read();

            if (c == '%') {
                skipWhitespace();
                String label = parseTextToken().trim();

                if (BibtexDatabaseWriter.DATABASE_ID_PREFIX.equals(label)) {
                    skipWhitespace();
                    database.setSharedDatabaseID(parseTextToken().trim());
                }
            } else if (c == '@') {
                unread(c);
                break;
            }
        }
    }

    private ParserResult parseFileContent() throws IOException {
        Map<String, String> meta = new HashMap<>();

        while (!eof) {
            boolean found = consumeUncritically('@');
            if (!found) {
                break;
            }

            skipWhitespace();

            // Try to read the entry type
            String entryType = parseTextToken().toLowerCase(Locale.ROOT).trim();

            switch (entryType) {
                case "preamble" -> {
                    database.setPreamble(parsePreamble());
                    // Consume a new line which separates the preamble from the next part (if the file was written with JabRef)
                    skipOneNewline();
                    // the preamble is saved verbatim anyway, so the text read so far can be dropped
                    dumpTextReadSoFarToString();
                }
                case "string" ->
                        parseBibtexString();
                case "comment" ->
                        parseJabRefComment(meta);
                default ->
                    // Not a comment, preamble, or string. Thus, it is an entry
                        parseAndAddEntry(entryType);
            }

            skipWhitespace();
        }

        addBibDeskGroupEntriesToJabRefGroups();

        try {
            MetaData metaData = metaDataParser.parse(
                    meta,
                    importFormatPreferences.bibEntryPreferences().getKeywordSeparator());
            if (bibDeskGroupTreeNode != null) {
                metaData.getGroups().ifPresentOrElse(existingGroupTree -> {
                            var existingGroups = meta.get(MetaData.GROUPSTREE);
                            // We only have one Group BibDeskGroup with n children
                            // instead of iterating through the whole group structure every time we just search in the metadata for the group name
                            var groupsToAdd = bibDeskGroupTreeNode.getChildren()
                                                                  .stream().
                                                                  filter(Predicate.not(groupTreeNode -> existingGroups.contains(GROUP_TYPE_SUFFIX + groupTreeNode.getName() + GROUP_QUOTE_CHAR)));
                            groupsToAdd.forEach(existingGroupTree::moveTo);
                        },
                        // metadata does not contain any groups, so we need to create an AllEntriesGroup and add the other groups as children
                        () -> {
                            GroupTreeNode rootNode = new GroupTreeNode(DefaultGroupsFactory.getAllEntriesGroup());
                            bibDeskGroupTreeNode.moveTo(rootNode);
                            metaData.setGroups(rootNode);
                        }
                );
            }
            parserResult.setMetaData(metaData);
        } catch (ParseException exception) {
            parserResult.addException(exception);
        }

        parseRemainingContent();

        checkEpilog();

        return parserResult;
    }

    private void checkEpilog() {
        // This is an incomplete and inaccurate try to verify if something went wrong with previous parsing activity even though there were no warnings so far
        // regex looks for something like 'identifier = blabla ,'
        if (!parserResult.hasWarnings() && Pattern.compile("\\w+\\s*=.*,").matcher(database.getEpilog()).find()) {
            parserResult.addWarning("following BibTex fragment has not been parsed:\n" + database.getEpilog());
        }
    }

    private void parseRemainingContent() {
        database.setEpilog(dumpTextReadSoFarToString().trim());
    }

    private void parseAndAddEntry(String type) {
        try {
            // collect all comments and the entry type definition in front of the actual entry
            // this is at least `@Type`
            String commentsAndEntryTypeDefinition = dumpTextReadSoFarToString();

            // remove first newline
            // this is appended by JabRef during writing automatically
            if (commentsAndEntryTypeDefinition.startsWith("\r\n")) {
                commentsAndEntryTypeDefinition = commentsAndEntryTypeDefinition.substring(2);
            } else if (commentsAndEntryTypeDefinition.startsWith("\n")) {
                commentsAndEntryTypeDefinition = commentsAndEntryTypeDefinition.substring(1);
            }

            BibEntry entry = parseEntry(type);
            // store comments collected without type definition
            entry.setCommentsBeforeEntry(
                    commentsAndEntryTypeDefinition.substring(0, commentsAndEntryTypeDefinition.lastIndexOf('@')));

            // store complete parsed serialization (comments, type definition + type contents)

            String parsedSerialization = commentsAndEntryTypeDefinition + dumpTextReadSoFarToString();
            entry.setParsedSerialization(parsedSerialization);

            database.insertEntry(entry);
        } catch (IOException ex) {
            // This makes the parser more robust:
            // If an exception is thrown when parsing an entry, drop the entry and try to resume parsing.
            LOGGER.warn("Could not parse entry", ex);
            parserResult.addWarning(Localization.lang("Error occurred when parsing entry") + ": '" + ex.getMessage()
                    + "'. " + "\n\n" + Localization.lang("JabRef skipped the entry."));
        }
    }

    private void parseJabRefComment(Map<String, String> meta) {
        StringBuilder buffer;
        try {
            buffer = parseBracketedFieldContent();
        } catch (IOException e) {
            // if we get an IO Exception here, then we have an unbracketed comment,
            // which means that we should just return and the comment will be picked up as arbitrary text
            // by the parser
            LOGGER.info("Found unbracketed comment");
            return;
        }

        // We remove all line breaks in the metadata
        // These have been inserted to prevent too long lines when the file was saved, and are not part of the data.
        String comment = buffer.toString().replaceAll("[\\x0d\\x0a]", "");
        if (comment.startsWith(MetaData.META_FLAG)) {
            String rest = comment.substring(MetaData.META_FLAG.length());

            int pos = rest.indexOf(':');

            if (pos > 0) {
                meta.put(rest.substring(0, pos), rest.substring(pos + 1));

                // meta comments are always re-written by JabRef and not stored in the file
                dumpTextReadSoFarToString();
            }
        } else if (comment.startsWith(MetaData.ENTRYTYPE_FLAG)) {
            // A custom entry type can also be stored in a
            // "@comment"
            Optional<BibEntryType> typ = MetaDataParser.parseCustomEntryType(comment);
            if (typ.isPresent()) {
                entryTypes.add(typ.get());
            } else {
                parserResult.addWarning(Localization.lang("Ill-formed entrytype comment in BIB file") + ": " + comment);
            }

            // custom entry types are always re-written by JabRef and not stored in the file
            dumpTextReadSoFarToString();
        } else if (comment.startsWith(MetaData.BIBDESK_STATIC_FLAG)) {
            try {
                parseBibDeskComment(comment, meta);
            } catch (ParseException ex) {
                parserResult.addException(ex);
            }
        }
    }

    /**
     * Adds BibDesk group entries to the JabRef database
     */
    private void addBibDeskGroupEntriesToJabRefGroups() {
        for (String groupName : parsedBibdeskGroups.keySet()) {
            String[] citationKeys = parsedBibdeskGroups.get(groupName).split(",");
            for (String citation : citationKeys) {
                Optional<BibEntry> bibEntry = database.getEntryByCitationKey(citation);
                Optional<String> groupValue = bibEntry.flatMap(entry -> entry.getField(StandardField.GROUPS));
                if (groupValue.isEmpty()) { // if the citation does not belong to a group already
                    bibEntry.flatMap(entry -> entry.setField(StandardField.GROUPS, groupName));
                } else if (!groupValue.get().contains(groupName)) {
                    // if the citation does belong to a group already and is not yet assigned to the same group, we concatenate
                    String concatGroup = groupValue.get() + "," + groupName;
                    bibEntry.flatMap(entryByCitationKey -> entryByCitationKey.setField(StandardField.GROUPS, concatGroup));
                }
            }
        }
    }

    /**
     * Parses comment types found in BibDesk, to migrate BibDesk Static Groups to JabRef.
     */
    private void parseBibDeskComment(String comment, Map<String, String> meta) throws ParseException {
        String xml = comment.substring(MetaData.BIBDESK_STATIC_FLAG.length() + 1, comment.length() - 1);
        try {
            // Build a document to handle the xml tags
            Document doc = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
            doc.getDocumentElement().normalize();

            NodeList dictList = doc.getElementsByTagName("dict");
            meta.putIfAbsent(MetaData.DATABASE_TYPE, "bibtex;");
            bibDeskGroupTreeNode = GroupTreeNode.fromGroup(new ExplicitGroup(BIB_DESK_ROOT_GROUP_NAME, GroupHierarchyType.INDEPENDENT, importFormatPreferences.bibEntryPreferences().getKeywordSeparator()));

            // Since each static group has their own dict element, we iterate through them
            for (int i = 0; i < dictList.getLength(); i++) {
                Element dictElement = (Element) dictList.item(i);
                NodeList keyList = dictElement.getElementsByTagName("key");
                NodeList stringList = dictElement.getElementsByTagName("string");

                String groupName = null;
                String citationKeys = null;

                // Retrieves group name and group entries and adds these to the metadata
                for (int j = 0; j < keyList.getLength(); j++) {
                    if (keyList.item(j).getTextContent().matches("group name")) {
                        groupName = stringList.item(j).getTextContent();
                        var staticGroup = new ExplicitGroup(groupName, GroupHierarchyType.INDEPENDENT, importFormatPreferences.bibEntryPreferences().getKeywordSeparator());
                        bibDeskGroupTreeNode.addSubgroup(staticGroup);
                    } else if (keyList.item(j).getTextContent().matches("keys")) {
                        citationKeys = stringList.item(j).getTextContent(); // adds group entries
                    }
                }
                // Adds the group name and citation keys to the field so all the entries can be added in the groups once parsed
                parsedBibdeskGroups.putIfAbsent(groupName, citationKeys);
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new ParseException(e);
        }
    }

    private void parseBibtexString() throws IOException {
        BibtexString bibtexString = parseString();
        try {
            database.addString(bibtexString);
        } catch (KeyCollisionException ex) {
            parserResult.addWarning(Localization.lang("Duplicate string name: '%0'", bibtexString.getName()));
        }
    }

    /**
     * Puts all text that has been read from the reader, including newlines, etc., since the last call of this method into a string. Removes the JabRef file header, if it is found
     *
     * @return the text read so far
     */
    private String dumpTextReadSoFarToString() {
        String result = getPureTextFromFile();
        int indexOfAt = result.indexOf("@");

        // if there is no entry found, simply return the content (necessary to parse text remaining after the last entry)
        if (indexOfAt == -1) {
            return purgeEOFCharacters(result);
        } else if (result.contains(BibtexDatabaseWriter.DATABASE_ID_PREFIX)) {
            return purge(result, BibtexDatabaseWriter.DATABASE_ID_PREFIX);
        } else if (result.contains(SaveConfiguration.ENCODING_PREFIX)) {
            return purge(result, SaveConfiguration.ENCODING_PREFIX);
        } else {
            return result;
        }
    }

    /**
     * Purges the given stringToPurge (if it exists) from the given context
     *
     * @return a stripped version of the context
     */
    private String purge(String context, String stringToPurge) {
        // purge the given string line if it exists
        int runningIndex = context.indexOf(stringToPurge);
        int indexOfAt = context.indexOf("@");
        while (runningIndex < indexOfAt) {
            if (context.charAt(runningIndex) == '\n') {
                break;
            } else if (context.charAt(runningIndex) == '\r') {
                if (context.charAt(runningIndex + 1) == '\n') {
                    runningIndex++;
                }
                break;
            }
            runningIndex++;
        }
        // strip empty lines
        while ((runningIndex < indexOfAt) &&
                ((context.charAt(runningIndex) == '\r') ||
                        (context.charAt(runningIndex) == '\n'))) {
            runningIndex++;
        }
        return context.substring(runningIndex);
    }

    private String getPureTextFromFile() {
        StringBuilder entry = new StringBuilder();
        while (!pureTextFromFile.isEmpty()) {
            entry.append(pureTextFromFile.pollFirst());
        }

        return entry.toString();
    }

    /**
     * Removes all eof characters from a StringBuilder and returns a new String with the resulting content
     *
     * @return a String without eof characters
     */
    private String purgeEOFCharacters(String input) {
        StringBuilder remainingText = new StringBuilder();
        for (Character character : input.toCharArray()) {
            if (!isEOFCharacter(character)) {
                remainingText.append(character);
            }
        }

        return remainingText.toString();
    }

    private void skipWhitespace() throws IOException {
        int character;

        while (true) {
            character = read();
            if (isEOFCharacter(character)) {
                eof = true;
                return;
            }

            if (!Character.isWhitespace((char) character)) {
                // found non-whitespace char
                unread(character);
                break;
            }
        }
    }

    private void skipSpace() throws IOException {
        int character;

        while (true) {
            character = read();
            if (isEOFCharacter(character)) {
                eof = true;
                return;
            }

            if ((char) character != ' ') {
                // found non-space char
                unread(character);
                break;
            }
        }
    }

    private void skipOneNewline() throws IOException {
        skipSpace();
        if (peek() == '\r') {
            read();
        }
        if (peek() == '\n') {
            read();
        }
    }

    private boolean isEOFCharacter(int character) {
        return (character == -1) || (character == 65535);
    }

    private String skipAndRecordWhitespace(int character) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        if (character != ' ') {
            stringBuilder.append((char) character);
        }
        while (true) {
            int nextCharacter = read();
            if (isEOFCharacter(nextCharacter)) {
                eof = true;
                return stringBuilder.toString();
            }

            if (Character.isWhitespace((char) nextCharacter)) {
                if (nextCharacter != ' ') {
                    stringBuilder.append((char) nextCharacter);
                }
            } else {
                // found non-whitespace char
                unread(nextCharacter);
                break;
            }
        }
        return stringBuilder.toString();
    }

    private int peek() throws IOException {
        int character = read();
        unread(character);

        return character;
    }

    private char[] peekTwoCharacters() throws IOException {
        char character1 = (char) read();
        char character2 = (char) read();
        unread(character2);
        unread(character1);
        return new char[] {
                character1, character2
        };
    }

    private int read() throws IOException {
        int character = pushbackReader.read();

        if (!isEOFCharacter(character)) {
            pureTextFromFile.offerLast((char) character);
        }
        if (character == '\n') {
            line++;
        }
        return character;
    }

    private void unread(int character) throws IOException {
        if (character == '\n') {
            line--;
        }
        pushbackReader.unread(character);
        if (pureTextFromFile.getLast() == character) {
            pureTextFromFile.pollLast();
        }
    }

    private BibtexString parseString() throws IOException {
        skipWhitespace();
        consume('{', '(');
        skipWhitespace();
        LOGGER.debug("Parsing string name");
        String name = parseTextToken();
        LOGGER.debug("Parsed string name");
        skipWhitespace();
        LOGGER.debug("Now the contents");
        consume('=');
        String content = parseFieldContent(FieldFactory.parseField(name));
        LOGGER.debug("Now I'm going to consume a }");
        consume('}', ')');
        // Consume new line which signals end of entry
        skipOneNewline();
        LOGGER.debug("Finished string parsing.");

        return new BibtexString(name, content, dumpTextReadSoFarToString());
    }

    private String parsePreamble() throws IOException {
        skipWhitespace();
        String result = parseBracketedText();
        // also "include" the newline in the preamble
        skipOneNewline();
        return result;
    }

    private BibEntry parseEntry(String entryType) throws IOException {
        BibEntry result = new BibEntry(EntryTypeFactory.parse(entryType));

        skipWhitespace();
        consume('{', '(');
        int character = peek();
        if ((character != '\n') && (character != '\r')) {
            skipWhitespace();
        }
        String key = parseKey();
        result.setCitationKey(key);
        skipWhitespace();

        while (true) {
            character = peek();
            if ((character == '}') || (character == ')')) {
                break;
            }

            if (character == ',') {
                consume(',');
            }

            skipWhitespace();

            character = peek();
            if ((character == '}') || (character == ')')) {
                break;
            }
            parseField(result);
        }

        consume('}', ')');

        // Consume new line which signals end of entry
        skipOneNewline();

        return result;
    }

    private void parseField(BibEntry entry) throws IOException {
        Field field = FieldFactory.parseField(parseTextToken().toLowerCase(Locale.ROOT));

        skipWhitespace();
        consume('=');
        String content = parseFieldContent(field);
        if (!content.isEmpty()) {
            if (entry.hasField(field)) {
                // The following hack enables the parser to deal with multiple
                // author or editor lines, stringing them together instead of getting just
                // one of them.
                // Multiple author or editor lines are not allowed by the bibtex
                // format, but at least one online database exports bibtex likes to do that, making
                // it inconvenient for users if JabRef did not accept it.
                if (field.getProperties().contains(FieldProperty.PERSON_NAMES)) {
                    entry.setField(field, entry.getField(field).orElse("") + " and " + content);
                } else if (StandardField.KEYWORDS == field) {
                    // TODO: multiple keywords fields should be combined to one
                    entry.addKeyword(content, importFormatPreferences.bibEntryPreferences().getKeywordSeparator());
                }
            } else {
                // If a BibDesk File Field is encountered
                if (field.getName().length() > 10 && field.getName().startsWith("bdsk-file-")) {
                    try {
                        byte[] decodedBytes = Base64.getDecoder().decode(content);

                        // Parse the base64 encoded binary plist to get the relative (to the .bib file) path
                        NSDictionary plist = (NSDictionary) BinaryPropertyListParser.parse(decodedBytes);

                        if (plist.containsKey("relativePath")) {
                            NSString relativePath = (NSString) plist.objectForKey("relativePath");
                            Path path = Path.of(relativePath.getContent());

                            LinkedFile file = new LinkedFile("", path, "");
                            entry.addFile(file);
                        } else {
                            LOGGER.error("Could not find attribute 'relativePath' for entry {} in decoded BibDesk field bdsk-file...) ", entry);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Could not parse Bibdesk files content (field: bdsk-file...) for entry {}", entry, e);
                    }
                } else {
                    entry.setField(field, content);
                }
            }
        }
    }

    private String parseFieldContent(Field field) throws IOException {
        skipWhitespace();
        StringBuilder value = new StringBuilder();
        int character;

        while (((character = peek()) != ',') && (character != '}') && (character != ')')) {
            if (eof) {
                throw new IOException("Error in line " + line + ": EOF in mid-string");
            }
            if (character == '"') {
                StringBuilder text = parseQuotedFieldExactly();
                value.append(text.toString());
            } else if (character == '{') {
                // Value is a string enclosed in brackets. There can be pairs
                // of brackets inside a field, so we need to count the
                // brackets to know when the string is finished.
                StringBuilder text = parseBracketedFieldContent();
                value.append(text.toString());
            } else if (Character.isDigit((char) character)) { // value is a number
                String number = parseTextToken();
                value.append(number);
            } else if (character == '#') {
                // Here, we hit the case of BibTeX string concatenation. E.g., "author = Kopp # Kolb".
                // We did NOT hit org.jabref.logic.bibtex.FieldWriter#BIBTEX_STRING_START_END_SYMBOL
                // See also ADR-0024
                consume('#');
            } else {
                String textToken = parseTextToken();
                if (textToken.isEmpty()) {
                    throw new IOException("Error in line " + line + " or above: "
                            + "Empty text token.\nThis could be caused " + "by a missing comma between two fields.");
                }
                value.append(FieldWriter.BIBTEX_STRING_START_END_SYMBOL).append(textToken).append(FieldWriter.BIBTEX_STRING_START_END_SYMBOL);
            }
            skipWhitespace();
        }
        return value.toString();
    }

    /**
     * This method is used to parse string labels, field names, entry type and numbers outside brackets.
     */
    private String parseTextToken() throws IOException {
        StringBuilder token = new StringBuilder(20);

        while (true) {
            int character = read();
            if (character == -1) {
                eof = true;
                return token.toString();
            }

            if (Character.isLetterOrDigit((char) character) || (":-_*+./'".indexOf(character) >= 0)) {
                token.append((char) character);
            } else {
                unread(character);
                return token.toString();
            }
        }
    }

    /**
     * Tries to restore the key
     *
     * @return rest of key on success, otherwise empty string
     * @throws IOException on Reader-Error
     */
    private String fixKey() throws IOException {
        StringBuilder key = new StringBuilder();
        int lookaheadUsed = 0;
        char currentChar;

        // Find a char which ends key (','&&'\n') or entryfield ('='):
        do {
            currentChar = (char) read();
            key.append(currentChar);
            lookaheadUsed++;
        } while ((currentChar != ',') && (currentChar != '\n') && (currentChar != '=')
                && (lookaheadUsed < BibtexParser.LOOKAHEAD));

        // Consumed a char too much, back into reader and remove from key:
        unread(currentChar);
        key.deleteCharAt(key.length() - 1);

        // Restore if possible:
        switch (currentChar) {
            case '=':
                // Get entryfieldname, push it back and take rest as key
                key = key.reverse();

                boolean matchedAlpha = false;
                for (int i = 0; i < key.length(); i++) {
                    currentChar = key.charAt(i);

                    /// Skip spaces:
                    if (!matchedAlpha && (currentChar == ' ')) {
                        continue;
                    }
                    matchedAlpha = true;

                    // Begin of entryfieldname (e.g. author) -> push back:
                    unread(currentChar);
                    if ((currentChar == ' ') || (currentChar == '\n')) {
                        /*
                         * found whitespaces, entryfieldname completed -> key in
                         * keybuffer, skip whitespaces
                         */
                        StringBuilder newKey = new StringBuilder();
                        for (int j = i; j < key.length(); j++) {
                            currentChar = key.charAt(j);
                            if (!Character.isWhitespace(currentChar)) {
                                newKey.append(currentChar);
                            }
                        }

                        // Finished, now reverse newKey and remove whitespaces:
                        key = newKey.reverse();
                        parserResult.addWarning(
                                Localization.lang("Line %0: Found corrupted citation key %1.", String.valueOf(line), key.toString()));
                    }
                }
                break;

            case ',':
                parserResult.addWarning(
                        Localization.lang("Line %0: Found corrupted citation key %1 (contains whitespaces).", String.valueOf(line), key.toString()));
                break;

            case '\n':
                parserResult.addWarning(
                        Localization.lang("Line %0: Found corrupted citation key %1 (comma missing).", String.valueOf(line), key.toString()));
                break;

            default:

                // No more lookahead, give up:
                unreadBuffer(key);
                return "";
        }

        return removeWhitespaces(key).toString();
    }

    /**
     * returns a new <code>StringBuilder</code> which corresponds to <code>toRemove</code> without whitespaces
     */
    private StringBuilder removeWhitespaces(StringBuilder toRemove) {
        StringBuilder result = new StringBuilder();
        char current;
        for (int i = 0; i < toRemove.length(); ++i) {
            current = toRemove.charAt(i);
            if (!Character.isWhitespace(current)) {
                result.append(current);
            }
        }
        return result;
    }

    /**
     * pushes buffer back into input
     *
     * @throws IOException can be thrown if buffer is bigger than LOOKAHEAD
     */
    private void unreadBuffer(StringBuilder stringBuilder) throws IOException {
        for (int i = stringBuilder.length() - 1; i >= 0; --i) {
            unread(stringBuilder.charAt(i));
        }
    }

    /**
     * This method is used to parse the citation key of an entry.
     */
    private String parseKey() throws IOException {
        StringBuilder token = new StringBuilder(20);

        while (true) {
            int character = read();

            if (character == -1) {
                eof = true;

                return token.toString();
            }

            if (!Character.isWhitespace((char) character) && (Character.isLetterOrDigit((char) character)
                    || (character == ':') || ("#{}~,=\uFFFD".indexOf(character) == -1))) {
                token.append((char) character);
            } else {
                if (Character.isWhitespace((char) character)) {
                    // We have encountered white space instead of the comma at
                    // the end of
                    // the key. Possibly the comma is missing, so we try to
                    // return what we
                    // have found, as the key and try to restore the rest in fixKey().
                    return token + fixKey();
                } else if ((character == ',') || (character == '}')) {
                    unread(character);
                    return token.toString();
                } else if (character == '=') {
                    // If we find a '=' sign, it is either an error, or
                    // the entry lacked a comma signifying the end of the key.
                    return token.toString();
                } else {
                    throw new IOException("Error in line " + line + ":" + "Character '" + (char) character + "' is not "
                            + "allowed in citation keys.");
                }
            }
        }
    }

    private String parseBracketedText() throws IOException {
        StringBuilder value = new StringBuilder();

        consume('{', '(');

        int brackets = 0;

        while (!((isClosingBracketNext()) && (brackets == 0))) {
            int character = read();
            if (isEOFCharacter(character)) {
                throw new IOException("Error in line " + line + ": EOF in mid-string");
            } else if ((character == '{') || (character == '(')) {
                brackets++;
            } else if ((character == '}') || (character == ')')) {
                brackets--;
            }

            // If we encounter whitespace of any kind, read it as a
            // simple space, and ignore any others that follow immediately.
            /*
             * if (j == '\n') { if (peek() == '\n') value.append('\n'); } else
             */
            if (Character.isWhitespace((char) character)) {
                String whitespacesReduced = skipAndRecordWhitespace(character);

                if (!whitespacesReduced.isEmpty() && !"\n\t".equals(whitespacesReduced)) { // &&
                    whitespacesReduced = whitespacesReduced.replace("\t", ""); // Remove tabulators.
                    value.append(whitespacesReduced);
                } else {
                    value.append(' ');
                }
            } else {
                value.append((char) character);
            }
        }

        consume('}', ')');

        return value.toString();
    }

    private boolean isClosingBracketNext() {
        try {
            int peek = peek();
            boolean isCurlyBracket = peek == '}';
            boolean isRoundBracket = peek == ')';
            return isCurlyBracket || isRoundBracket;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * This is called if a field in the form of <code>field = {content}</code> is parsed.
     * The global variable <code>character</code> contains <code>{</code>.
     */
    private StringBuilder parseBracketedFieldContent() throws IOException {
        StringBuilder value = new StringBuilder();

        consume('{');

        int brackets = 0;
        char character;
        char lastCharacter = '\0';

        while (true) {
            character = (char) read();

            boolean isClosingBracket = false;
            if (character == '}') {
                if (lastCharacter == '\\') {
                    // We hit `\}`
                    // It could be that a user has a backslash at the end of the entry, but intended to put a file path
                    // We want to be relaxed at that case
                    // First described at https://github.com/JabRef/jabref/issues/9668
                    char[] nextTwoCharacters = peekTwoCharacters();
                    // Check for "\},\n" - Example context: `  path = {c:\temp\},\n`
                    // On Windows, it could be "\},\r\n", thus we rely in OS.NEWLINE.charAt(0) (which returns '\r' or '\n').
                    //   In all cases, we should check for '\n' as the file could be encoded with Linux line endings on Windows.
                    // We hit '\}\r` or `\}\n`
                    // Heuristics: Unwanted escaping of }
                    //
                    // Two consequences:
                    //
                    // 1. Keep `\` as read
                    //   This is already done
                    //
                    // 2. Treat `}` as closing bracket
                    isClosingBracket = (nextTwoCharacters[0] == ',') && ((nextTwoCharacters[1] == OS.NEWLINE.charAt(0)) || (nextTwoCharacters[1] == '\n'));
                } else {
                    isClosingBracket = true;
                }
            }

            if (isClosingBracket && (brackets == 0)) {
                return value;
            } else if (isEOFCharacter(character)) {
                throw new IOException("Error in line " + line + ": EOF in mid-string");
            } else if ((character == '{') && (!isEscapeSymbol(lastCharacter))) {
                brackets++;
            } else if (isClosingBracket) {
                brackets--;
            }

            value.append(character);

            lastCharacter = character;
        }
    }

    private boolean isEscapeSymbol(char character) {
        return '\\' == character;
    }

    private StringBuilder parseQuotedFieldExactly() throws IOException {
        StringBuilder value = new StringBuilder();

        consume('"');

        int brackets = 0;

        while (!((peek() == '"') && (brackets == 0))) {
            int j = read();
            if (isEOFCharacter(j)) {
                throw new IOException("Error in line " + line + ": EOF in mid-string");
            } else if (j == '{') {
                brackets++;
            } else if (j == '}') {
                brackets--;
            }
            value.append((char) j);
        }

        consume('"');

        return value;
    }

    private void consume(char expected) throws IOException {
        int character = read();

        if (character != expected) {
            throw new IOException(
                    "Error in line " + line + ": Expected " + expected + " but received " + (char) character);
        }
    }

    private boolean consumeUncritically(char expected) throws IOException {
        int character;
        do {
            character = read();
        } while ((character != expected) && (character != -1) && (character != 65535));

        if (isEOFCharacter(character)) {
            eof = true;
        }

        // Return true if we actually found the character we were looking for:
        return character == expected;
    }

    private void consume(char firstOption, char secondOption) throws IOException {
        // Consumes one of the two, doesn't care which appears.

        int character = read();

        if ((character != firstOption) && (character != secondOption)) {
            throw new IOException("Error in line " + line + ": Expected " + firstOption + " or " + secondOption
                    + " but received " + (char) character);
        }
    }
}
