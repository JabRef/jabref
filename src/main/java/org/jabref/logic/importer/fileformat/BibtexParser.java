package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldContentParser;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for importing BibTeX-files.
 * <p>
 * Use:
 * <p>
 * BibtexParser parser = new BibtexParser(reader);
 * <p>
 * ParserResult result = parser.parse();
 * <p>
 * or
 * <p>
 * ParserResult result = BibtexParser.parse(reader);
 * <p>
 * Can be used stand-alone.
 */
public class BibtexParser implements Parser {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexParser.class);

    private static final Integer LOOKAHEAD = 64;
    private final FieldContentParser fieldContentParser;
    private final Deque<Character> pureTextFromFile = new LinkedList<>();
    private final ImportFormatPreferences importFormatPreferences;
    private PushbackReader pushbackReader;
    private BibDatabase database;
    private Map<String, EntryType> entryTypes;
    private boolean eof;
    private int line = 1;
    private ParserResult parserResult;
    private MetaDataParser metaDataParser;

    public BibtexParser(ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor) {
        this.importFormatPreferences = Objects.requireNonNull(importFormatPreferences);
        fieldContentParser = new FieldContentParser(importFormatPreferences.getFieldContentParserPreferences());
        metaDataParser = new MetaDataParser(fileMonitor);
    }

    /**
     * Parses BibtexEntries from the given string and returns one entry found (or null if none found)
     * <p>
     * It is undetermined which entry is returned, so use this in case you know there is only one entry in the string.
     *
     * @param bibtexString
     * @param fileMonitor
     * @return An Optional<BibEntry>. Optional.empty() if non was found or an error occurred.
     * @throws ParseException
     */
    public static Optional<BibEntry> singleFromString(String bibtexString, ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor) throws ParseException {
        Collection<BibEntry> entries = new BibtexParser(importFormatPreferences, fileMonitor).parseEntries(bibtexString);
        if ((entries == null) || entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(entries.iterator().next());
    }

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        return parseEntries(reader);
    }

    public List<BibEntry> parseEntries(Reader reader) throws ParseException {
        try {
            return parse(reader).getDatabase().getEntries();
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    public List<BibEntry> parseEntries(String bibtexString) throws ParseException {
        return parseEntries(new StringReader(bibtexString));
    }

    public Optional<BibEntry> parseSingleEntry(String bibtexString) throws ParseException {
        return parseEntries(bibtexString).stream().findFirst();
    }

    /**
     * Will parse the BibTex-Data found when reading from reader. Ignores any encoding supplied in the file by
     * "Encoding: myEncoding".
     * <p>
     * The reader will be consumed.
     * <p>
     * Multiple calls to parse() return the same results
     *
     * @return ParserResult
     * @throws IOException
     */
    public ParserResult parse(Reader in) throws IOException {
        Objects.requireNonNull(in);
        pushbackReader = new PushbackReader(in, BibtexParser.LOOKAHEAD);

        // Bibtex related contents.
        initializeParserResult();

        parseDatabaseID();

        skipWhitespace();

        try {
            return parseFileContent();
        } catch (KeyCollisionException kce) {
            throw new IOException("Duplicate ID in bibtex file: " + kce);
        }
    }

    private void initializeParserResult() {
        database = new BibDatabase();
        entryTypes = new HashMap<>(); // To store custom entry types parsed.
        parserResult = new ParserResult(database, new MetaData(), entryTypes);
    }

    private void parseDatabaseID() throws IOException {

        while (!eof) {
            skipWhitespace();
            char c = (char) read();

            if (c == '%') {
                skipWhitespace();
                String label = parseTextToken().trim();

                if (label.equals(BibtexDatabaseWriter.DATABASE_ID_PREFIX)) {
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

            if ("preamble".equals(entryType)) {
                database.setPreamble(parsePreamble());
                // Consume new line which signals end of preamble
                skipOneNewline();
                // the preamble is saved verbatim anyways, so the text read so far can be dropped
                dumpTextReadSoFarToString();
            } else if ("string".equals(entryType)) {
                parseBibtexString();
            } else if ("comment".equals(entryType)) {
                parseJabRefComment(meta);
            } else {
                // Not a comment, preamble, or string. Thus, it is an entry
                parseAndAddEntry(entryType);
            }

            skipWhitespace();
        }

        // Instantiate meta data:
        try {
            parserResult.setMetaData(metaDataParser.parse(meta, importFormatPreferences.getKeywordSeparator()));
        } catch (ParseException exception) {
            parserResult.addException(exception);
        }

        parseRemainingContent();

        return parserResult;
    }

    private void parseRemainingContent() {
        database.setEpilog(dumpTextReadSoFarToString().trim());
    }

    private void parseAndAddEntry(String type) {
        /**
         * Morten Alver 13 Aug 2006: Trying to make the parser more
         * robust. If an exception is thrown when parsing an entry,
         * drop the entry and try to resume parsing. Add a warning
         * for the user.
         */
        try {
            // collect all comments and the entry type definition in front of the actual entry
            // this is at least `@Type`
            String commentsAndEntryTypeDefinition = dumpTextReadSoFarToString();

            BibEntry entry = parseEntry(type);
            // store comments collected without type definition
            entry.setCommentsBeforeEntry(
                    commentsAndEntryTypeDefinition.substring(0, commentsAndEntryTypeDefinition.lastIndexOf('@')));
            // store complete parsed serialization (comments, type definition + type contents)
            entry.setParsedSerialization(commentsAndEntryTypeDefinition + dumpTextReadSoFarToString());

            boolean duplicateKey = database.insertEntry(entry);
            if (duplicateKey) {
                parserResult.addDuplicateKey(entry.getCiteKey());
            }
        } catch (IOException ex) {
            LOGGER.debug("Could not parse entry", ex);
            parserResult.addWarning(Localization.lang("Error occurred when parsing entry") + ": '" + ex.getMessage()
                    + "'. " + Localization.lang("Skipped entry."));

        }
    }

    private void parseJabRefComment(Map<String, String> meta) {
        StringBuilder buffer = null;
        try {
            buffer = parseBracketedTextExactly();
        } catch (IOException e) {
            /* if we get an IO Exception here, than we have an unbracketed comment,
            * which means that we should just return and the comment will be picked up as arbitrary text
            *  by the parser
             */
            LOGGER.info("Found unbracketed comment");
            return;
        }

        String comment = buffer.toString().replaceAll("[\\x0d\\x0a]", "");
        if (comment.substring(0, Math.min(comment.length(), MetaData.META_FLAG.length())).equals(MetaData.META_FLAG)) {

            if (comment.substring(0, MetaData.META_FLAG.length()).equals(MetaData.META_FLAG)) {
                String rest = comment.substring(MetaData.META_FLAG.length());

                int pos = rest.indexOf(':');

                if (pos > 0) {
                    // We remove all line breaks in the metadata - these
                    // will have been inserted
                    // to prevent too long lines when the file was
                    // saved, and are not part of the data.
                    meta.put(rest.substring(0, pos), rest.substring(pos + 1));

                    // meta comments are always re-written by JabRef and not stored in the file
                    dumpTextReadSoFarToString();
                }
            }
        } else if (comment.substring(0, Math.min(comment.length(), CustomEntryType.ENTRYTYPE_FLAG.length()))
                .equals(CustomEntryType.ENTRYTYPE_FLAG)) {
            // A custom entry type can also be stored in a
            // "@comment"
            Optional<CustomEntryType> typ = CustomEntryType.parse(comment);
            if (typ.isPresent()) {
                entryTypes.put(typ.get().getName(), typ.get());
            } else {
                parserResult.addWarning(Localization.lang("Ill-formed entrytype comment in BIB file") + ": " + comment);
            }

            // custom entry types are always re-written by JabRef and not stored in the file
            dumpTextReadSoFarToString();
        }

    }

    private void parseBibtexString() throws IOException {
        BibtexString bibtexString = parseString();
        bibtexString.setParsedSerialization(dumpTextReadSoFarToString());
        try {
            database.addString(bibtexString);
        } catch (KeyCollisionException ex) {
            parserResult.addWarning(Localization.lang("Duplicate string name") + ": " + bibtexString.getName());
        }
    }

    /**
     * Puts all text that has been read from the reader, including newlines, etc., since the last call of this method into a string.
     * Removes the JabRef file header, if it is found
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
        } else if (result.contains(SavePreferences.ENCODING_PREFIX)) {
            return purge(result, SavePreferences.ENCODING_PREFIX);
        } else {
            return result;
        }
    }

    private String purge(String context, String stringToPurge) {
        // purge the encoding line if it exists
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
        return context.substring(runningIndex + 1);
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
            if (!(isEOFCharacter(character))) {
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
        String content = parseFieldContent(name);
        LOGGER.debug("Now I'm going to consume a }");
        consume('}', ')');
        // Consume new line which signals end of entry
        skipOneNewline();
        LOGGER.debug("Finished string parsing.");

        return new BibtexString(name, content);
    }

    private String parsePreamble() throws IOException {
        skipWhitespace();
        return parseBracketedText();

    }

    private BibEntry parseEntry(String entryType) throws IOException {
        BibEntry result = new BibEntry(entryType);
        skipWhitespace();
        consume('{', '(');
        int character = peek();
        if ((character != '\n') && (character != '\r')) {
            skipWhitespace();
        }
        String key = parseKey();
        result.setCiteKey(key);
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
        String key = parseTextToken().toLowerCase(Locale.ROOT);

        skipWhitespace();
        consume('=');
        String content = parseFieldContent(key);
        if (!content.isEmpty()) {
            if (entry.hasField(key)) {
                // The following hack enables the parser to deal with multiple
                // author or
                // editor lines, stringing them together instead of getting just
                // one of them.
                // Multiple author or editor lines are not allowed by the bibtex
                // format, but
                // at least one online database exports bibtex like that, making
                // it inconvenient
                // for users if JabRef did not accept it.
                if (InternalBibtexFields.getFieldProperties(key).contains(FieldProperty.PERSON_NAMES)) {
                    entry.setField(key, entry.getField(key).get() + " and " + content);
                } else if (FieldName.KEYWORDS.equals(key)) {
                    //multiple keywords fields should be combined to one
                    entry.addKeyword(content, importFormatPreferences.getKeywordSeparator());
                }
            } else {
                entry.setField(key, content);
            }
        }
    }

    private String parseFieldContent(String key) throws IOException {
        skipWhitespace();
        StringBuilder value = new StringBuilder();
        int character;

        while (((character = peek()) != ',') && (character != '}') && (character != ')')) {

            if (eof) {
                throw new IOException("Error in line " + line + ": EOF in mid-string");
            }
            if (character == '"') {
                StringBuilder text = parseQuotedFieldExactly();
                value.append(fieldContentParser.format(text, key));
            } else if (character == '{') {
                // Value is a string enclosed in brackets. There can be pairs
                // of brackets inside of a field, so we need to count the
                // brackets to know when the string is finished.
                StringBuilder text = parseBracketedTextExactly();
                value.append(fieldContentParser.format(text, key));

            } else if (Character.isDigit((char) character)) { // value is a number
                String number = parseTextToken();
                value.append(number);
            } else if (character == '#') {
                consume('#');
            } else {
                String textToken = parseTextToken();
                if (textToken.isEmpty()) {
                    throw new IOException("Error in line " + line + " or above: "
                            + "Empty text token.\nThis could be caused " + "by a missing comma between two fields.");
                }
                value.append('#').append(textToken).append('#');
            }
            skipWhitespace();
        }
        return value.toString();

    }

    /**
     * This method is used to parse string labels, field names, entry type and
     * numbers outside brackets.
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
                    parserResult.addWarning(
                            Localization.lang("Line %0: Found corrupted BibTeX key.", String.valueOf(line)));
                    key = newKey.reverse();
                }
            }
            break;

        case ',':
            parserResult.addWarning(Localization.lang("Line %0: Found corrupted BibTeX key (contains whitespaces).",
                    String.valueOf(line)));
            break;

        case '\n':
            parserResult.addWarning(
                    Localization.lang("Line %0: Found corrupted BibTeX key (comma missing).", String.valueOf(line)));
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
     *
     * @param toRemove
     * @return
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
     * @param stringBuilder
     * @throws IOException can be thrown if buffer is bigger than LOOKAHEAD
     */
    private void unreadBuffer(StringBuilder stringBuilder) throws IOException {
        for (int i = stringBuilder.length() - 1; i >= 0; --i) {
            unread(stringBuilder.charAt(i));
        }
    }

    /**
     * This method is used to parse the bibtex key for an entry.
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
                            + "allowed in bibtex keys.");
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

                if (!(whitespacesReduced.isEmpty()) && !"\n\t".equals(whitespacesReduced)) { // &&
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

    private StringBuilder parseBracketedTextExactly() throws IOException {
        StringBuilder value = new StringBuilder();

        consume('{');

        int brackets = 0;
        char character;
        char lastCharacter = '\0';

        while (true) {
            character = (char) read();

            boolean isClosingBracket = (character == '}') && (lastCharacter != '\\');

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
