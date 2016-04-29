/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.bibtex.FieldProperties;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.IdGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class BibtexParser {
    private static final Log LOGGER = LogFactory.getLog(BibtexParser.class);

    private final PushbackReader pushbackReader;
    private BibDatabase database;
    private Map<String, EntryType> entryTypes;
    private boolean eof;
    private int line = 1;
    private final FieldContentParser fieldContentParser = new FieldContentParser();
    private ParserResult parserResult;
    private static final Integer LOOKAHEAD = 64;
    private final Deque<Character> pureTextFromFile = new LinkedList<>();


    public BibtexParser(Reader in) {
        Objects.requireNonNull(in);

        pushbackReader = new PushbackReader(in, BibtexParser.LOOKAHEAD);
    }

    /**
     * Shortcut usage to create a Parser and read the input.
     *
     * @param in the Reader to read from
     * @throws IOException
     */
    public static ParserResult parse(Reader in) throws IOException {
        BibtexParser parser = new BibtexParser(in);
        return parser.parse();
    }


    /**
     * Parses BibtexEntries from the given string and returns the collection of all entries found.
     *
     * @param bibtexString
     * @return Returns returns an empty collection if no entries where found or if an error occurred.
     */
    public static List<BibEntry> fromString(String bibtexString) {
        StringReader reader = new StringReader(bibtexString);
        BibtexParser parser = new BibtexParser(reader);

        try {
            return parser.parse().getDatabase().getEntries();
        } catch (Exception e) {
            LOGGER.warn("BibtexParser.fromString(String): " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Parses BibtexEntries from the given string and returns one entry found (or null if none found)
     * <p>
     * It is undetermined which entry is returned, so use this in case you know there is only one entry in the string.
     *
     * @param bibtexString
     * @return The BibEntry or null if non was found or an error occurred.
     */
    public static BibEntry singleFromString(String bibtexString) {
        Collection<BibEntry> entries = BibtexParser.fromString(bibtexString);
        if ((entries == null) || entries.isEmpty()) {
            return null;
        }
        return entries.iterator().next();
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
    public ParserResult parse() throws IOException {
        // If we already parsed this, just return it.
        if (parserResult != null) {
            return parserResult;
        }
        // Bibtex related contents.
        initializeParserResult();

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
        parserResult = new ParserResult(database, null, entryTypes);
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
            String entryType = parseTextToken().toLowerCase().trim();

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
            parserResult.setMetaData(new MetaData(meta));
        } catch (ParseException exception) {
            parserResult.addWarning(exception.getLocalizedMessage());
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
            BibEntry entry = parseEntry(type);

            boolean duplicateKey = database.insertEntry(entry);
            entry.setParsedSerialization(dumpTextReadSoFarToString());
            if (duplicateKey) {
                parserResult.addDuplicateKey(entry.getCiteKey());
            } else if ((entry.getCiteKey() == null) || entry.getCiteKey().isEmpty()) {
                parserResult.addWarning(Localization.lang("Empty BibTeX key") + ": " + entry.getAuthorTitleYear(40)
                        + " (" + Localization.lang("Grouping may not work for this entry.") + ")");
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not parse entry", ex);
            parserResult.addWarning(Localization.lang("Error occurred when parsing entry") + ": '"
                    + ex.getMessage() + "'. " + Localization.lang("Skipped entry."));

        }
    }

    private void parseJabRefComment(Map<String, String> meta) throws IOException {
        StringBuilder buffer = parseBracketedTextExactly();
        /**
         *
         * Metadata are used to store Bibkeeper-specific
         * information in .bib files.
         *
         * Metadata are stored in bibtex files in the format
         *
         * @comment{jabref-meta: type:data0;data1;data2;...}
         *
         * Each comment that starts with the META_FLAG is stored
         * in the meta HashMap, with type as key. Unluckily, the
         * old META_FLAG bibkeeper-meta: was used in JabRef 1.0
         * and 1.1, so we need to support it as well. At least
         * for a while. We'll always save with the new one.
         */
        String comment = buffer.toString().replaceAll("[\\x0d\\x0a]", "");
        if (comment.substring(0,
                Math.min(comment.length(), MetaData.META_FLAG.length())).equals(
                MetaData.META_FLAG)) {


            if (comment.substring(0, MetaData.META_FLAG.length()).equals(
                    MetaData.META_FLAG)) {
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
            CustomEntryType typ = CustomEntryTypesManager.parseEntryType(comment);
            if(typ == null) {
                parserResult.addWarning(Localization.lang("Ill-formed entrytype comment in bib file") + ": " +
                        comment);
            } else {
                entryTypes.put(typ.getName(), typ);
            }

            // custom entry types are always re-written by JabRef and not stored in the file
            dumpTextReadSoFarToString();
        } else {
            // FIXME: user comments are simply dropped
            // at least, we log that we ignored the comment
            LOGGER.info("Dropped comment from database: " + comment);
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
        } else {

            //skip all text except newlines and whitespaces before first @. This is necessary to remove the file header
            int runningIndex = indexOfAt - 1;
            while (runningIndex >= 0) {
                if (!Character.isWhitespace(result.charAt(runningIndex))) {
                    break;
                }
                runningIndex--;
            }

            if(runningIndex > -1) {
                // We have to ignore some text at the beginning
                // so we view the first line break as the end of the previous text and don't store it
                if(result.charAt(runningIndex + 1) == '\r') {
                    runningIndex++;
                }
                if(result.charAt(runningIndex + 1) == '\n') {
                    runningIndex++;
                }
            }

            return result.substring(runningIndex + 1);
        }
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
        if(peek() == '\r') {
            read();
        }
        if(peek() == '\n') {
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

        if(! isEOFCharacter(character)) {
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
        if(pureTextFromFile.getLast() == character) {
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

        String id = IdGenerator.next();
        return new BibtexString(id, name, content);
    }

    private String parsePreamble() throws IOException {
        skipWhitespace();
        return parseBracketedText().toString();

    }

    private BibEntry parseEntry(String entryType) throws IOException {
        String id = IdGenerator.next();
        BibEntry result = new BibEntry(id, entryType);
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
        String key = parseTextToken().toLowerCase();

        skipWhitespace();
        consume('=');
        String content = parseFieldContent(key);
        // Now, if the field in question is set up to be fitted automatically
        // with braces around
        // capitals, we should remove those now when reading the field:
        if (Globals.prefs.putBracesAroundCapitals(key)) {
            content = StringUtil.removeBracesAroundCapitals(content);
        }
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
                // for users if JabRef didn't accept it.
                if (InternalBibtexFields.getFieldExtras(key).contains(FieldProperties.PERSON_NAMES)) {
                    entry.setField(key, entry.getField(key) + " and " + content);
                } else if ("keywords".equals(key)) {
                    //multiple keywords fields should be combined to one
                    entry.addKeyword(content);
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
                            + "Empty text token.\nThis could be caused "
                            + "by a missing comma between two fields.");
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
                        parserResult.addWarning(Localization.lang("Line %0: Found corrupted BibTeX-key.",
                                String.valueOf(line)));
                        key = newKey.reverse();
                    }
                }
                break;

            case ',':
                parserResult.addWarning(Localization.lang("Line %0: Found corrupted BibTeX-key (contains whitespaces).",
                        String.valueOf(line)));
                break;

            case '\n':
                parserResult.addWarning(Localization.lang("Line %0: Found corrupted BibTeX-key (comma missing).",
                        String.valueOf(line)));
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
                    throw new IOException("Error in line " + line + ":" + "Character '" + (char) character
                            + "' is not " + "allowed in bibtex keys.");
                }

            }
        }

    }

    private StringBuffer parseBracketedText() throws IOException {
        StringBuffer value = new StringBuffer();

        consume('{','(');

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

        consume('}',')');

        return value;
    }

    private boolean isClosingBracketNext () {
        try {
            int peek = peek();
            boolean isCurlyBracket = peek == '}';
            boolean isRoundBracket = peek == ')';
            return isCurlyBracket || isRoundBracket;
        } catch(IOException e) {
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
            throw new IOException("Error in line " + line + ": Expected " + expected
                    + " but received " + (char) character);
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
            throw new IOException("Error in line " + line + ": Expected " + firstOption + " or "
                    + secondOption + " but received " + (char) character);
        }
    }
}
