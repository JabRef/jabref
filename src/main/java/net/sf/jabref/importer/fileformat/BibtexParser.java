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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

import net.sf.jabref.*;
import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.*;
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
    private BibtexDatabase database;
    private HashMap<String, EntryType> entryTypes;
    private boolean eof;
    private int line = 1;
    private final FieldContentParser fieldContentParser = new FieldContentParser();
    private ParserResult parserResult;
    private static final Integer LOOKAHEAD = 64;
    private final boolean autoDoubleBraces;
    private final Deque<Character> pureTextFromFile = new LinkedList<>();


    public BibtexParser(Reader in) {
        Objects.requireNonNull(in);

        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
        autoDoubleBraces = Globals.prefs.getBoolean(JabRefPreferences.AUTO_DOUBLE_BRACES);
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
     * @return Returns null if an error occurred, returns an empty collection if no entries where found.
     */
    public static Collection<BibtexEntry> fromString(String bibtexString) {
        StringReader reader = new StringReader(bibtexString);
        BibtexParser parser = new BibtexParser(reader);

        try {
            return parser.parse().getDatabase().getEntries();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses BibtexEntries from the given string and returns one entry found (or null if none found)
     * <p>
     * It is undetermined which entry is returned, so use this in case you know there is only one entry in the string.
     *
     * @param bibtexString
     * @return The bibtexentry or null if non was found or an error occurred.
     */
    public static BibtexEntry singleFromString(String bibtexString) {
        Collection<BibtexEntry> entries = BibtexParser.fromString(bibtexString);
        if ((entries == null) || entries.isEmpty()) {
            return null;
        }
        return entries.iterator().next();
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    public static boolean isRecognizedFormat(Reader reader) throws IOException {
        // Our strategy is to look for the "@<type>    {" line.
        BufferedReader in = new BufferedReader(reader);

        Pattern formatPattern = Pattern.compile("@[a-zA-Z]*\\s*\\{");

        String bibtexString;

        while ((bibtexString = in.readLine()) != null) {
            if (formatPattern.matcher(bibtexString).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Will parse the BibTex-Data found when reading from reader.
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
        database = new BibtexDatabase();
        entryTypes = new HashMap<>(); // To store custem entry types parsed.
        parserResult = new ParserResult(database, null, entryTypes);
    }

    private ParserResult parseFileContent() throws IOException {
        HashMap<String, String> meta = new HashMap<>();

        while (!eof) {
            boolean found = consumeUncritically('@');
            if (!found) {
                break;
            }

            skipWhitespace();

            // try to read the entry type
            String entryType = parseTextToken();
            EntryType type = EntryTypes.getType(entryType);
            MyEntryClass myClass = MyEntryClasses.getClassFor(entryType);
            boolean isEntry = type != null;

            // The entry type name was not recognized. This can mean
            // that it is a string, preamble, or comment. If so,
            // parse and set accordingly. If not, assume it is an entry
            // with an unknown type.
            if (!isEntry) {
                if ("preamble".equals(entryType.toLowerCase())) {
                    database.setPreamble(parsePreamble());
                    // the preamble is saved verbatim anyways, so the text read so far can be dropped
                    dumpTextReadSoFarToString();
                } else if ("string".equals(entryType.toLowerCase())) {
                    parseBibtexString();
                } else if ("comment".equals(entryType.toLowerCase())) {
                    parseJabRefComment(meta);
                } else {
                    // The entry type was not recognized. This may mean that
                    // it is a custom entry type whose definition will
                    // appear
                    // at the bottom of the file. So we use an
                    // UnknownEntryType
                    // to remember the type name by.
                    type = new UnknownEntryType(EntryUtil.capitalizeFirst(entryType));
                    isEntry = true;
                }
            }

            // True if not comment, preamble or string.
            if (isEntry) {
                parseAndAddEntry(type);
            }

            skipWhitespace();
        }
        // Before returning the database, update entries with unknown type
        // based on parsed type definitions, if possible.
        checkEntryTypes(parserResult);

        // Instantiate meta data:
        parserResult.setMetaData(new MetaData(meta, database));

        parseRemainingContent();

        return parserResult;
    }

    private void parseRemainingContent() {
        database.setEpilog(dumpTextReadSoFarToString());
    }

    private void parseAndAddEntry(EntryType type) {
        /**
         * Morten Alver 13 Aug 2006: Trying to make the parser more
         * robust. If an exception is thrown when parsing an entry,
         * drop the entry and try to resume parsing. Add a warning
         * for the user.
         */
        try {
            BibtexEntry entry = parseEntry(type);

            boolean duplicateKey = database.insertEntry(entry);
            entry.setParsedSerialization(dumpTextReadSoFarToString());
            if (duplicateKey) {
                parserResult.addDuplicateKey(entry.getCiteKey());
            } else if ((entry.getCiteKey() == null) || "".equals(entry.getCiteKey())) {
                parserResult
                        .addWarning(Localization.lang("Empty BibTeX key") + ": "
                                + entry.getAuthorTitleYear(40) + " ("
                                + Localization.lang("grouping may not work for this entry") + ")");
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not parse entry", ex);
            parserResult.addWarning(Localization.lang("Error occurred when parsing entry") + ": '"
                    + ex.getMessage() + "'. " + Localization.lang("Skipped entry."));

        }
    }

    private void parseJabRefComment(HashMap<String, String> meta) throws IOException {
        StringBuffer buffer = parseBracketedTextExactly();
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
            entryTypes.put(typ.getName(), typ);

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
        StringBuilder entry = new StringBuilder();
        while (!pureTextFromFile.isEmpty()) {
            entry.append(pureTextFromFile.pollFirst());
        }

        String result = entry.toString();
        int indexOfAt = entry.indexOf("@");

        // if there is no entry found, simply return the content (necessary to parse text remaining after the last entry)
        if (indexOfAt == -1) {
            return purgeEOFCharacters(entry);
        } else {

            //skip all text except newlines and whitespaces before first @. This is necessary to remove the file header
            int runningIndex = indexOfAt - 1;
            while (runningIndex >= 0) {
                if (!Character.isWhitespace(result.charAt(runningIndex))) {
                    break;
                }
                runningIndex--;
            }


            result = result.substring(runningIndex + 1);

            return result;
        }
    }

    /**
     * Removes all eof characters from a StringBuilder and returns a new String with the resulting content
     *
     * @return a String without eof characters
     */
    private String purgeEOFCharacters(StringBuilder input) {

        StringBuilder remainingText = new StringBuilder();
        for (Character character : input.toString().toCharArray()) {
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

            if (Character.isWhitespace((char) character)) {
                continue;
            } else {
                // found non-whitespace char
                unread(character);
                break;
            }
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
                continue;
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
        pureTextFromFile.offerLast(Character.valueOf((char) character));
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
        pureTextFromFile.pollLast();
    }

    private BibtexString parseString() throws IOException {
        skipWhitespace();
        consume('{', '(');
        // while (read() != '}');
        skipWhitespace();
        // Util.pr("Parsing string name");
        String name = parseTextToken();
        // Util.pr("Parsed string name");
        skipWhitespace();
        // Util.pr("Now the contents");
        consume('=');
        String content = parseFieldContent(name);
        // Util.pr("Now I'm going to consume a }");
        consume('}', ')');
        // Util.pr("Finished string parsing.");
        String id = IdGenerator.next();
        return new BibtexString(id, name, content);
    }

    private String parsePreamble() throws IOException {
        return parseBracketedText().toString();
    }

    private BibtexEntry parseEntry(EntryType entryType) throws IOException {
        String id = IdGenerator.next();
        BibtexEntry result = new BibtexEntry(id, entryType);
        skipWhitespace();
        consume('{', '(');
        int character = peek();
        if ((character != '\n') && (character != '\r')) {
            skipWhitespace();
        }
        String key = parseKey();

        result.setField(BibtexEntry.KEY_FIELD, key);
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
        return result;
    }

    private void parseField(BibtexEntry entry) throws IOException {
        String key = parseTextToken().toLowerCase();
        // Util.pr("Field: _"+key+"_");
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
            if (entry.getField(key) == null) {
                entry.setField(key, content);
            } else {
                // The following hack enables the parser to deal with multiple
                // author or
                // editor lines, stringing them together instead of getting just
                // one of them.
                // Multiple author or editor lines are not allowed by the bibtex
                // format, but
                // at least one online database exports bibtex like that, making
                // it inconvenient
                // for users if JabRef didn't accept it.
                if ("author".equals(key) || "editor".equals(key)) {
                    entry.setField(key, entry.getField(key) + " and " + content);
                }
            }
        }
    }

    private String parseFieldContent(String key) throws IOException {
        skipWhitespace();
        StringBuilder value = new StringBuilder();
        int character;

        while (((character = peek()) != ',') && (character != '}') && (character != ')')) {

            if (eof) {
                throw new RuntimeException("Error in line " + line + ": EOF in mid-string");
            }
            if (character == '"') {
                StringBuffer text = parseQuotedFieldExactly();
                value.append(fieldContentParser.format(text, key));
                /*
                 *
                 * The following code doesn't handle {"} correctly: // value is
                 * a string consume('"');
                 *
                 * while (!((peek() == '"') && (j != '\\'))) { j = read(); if
                 * (_eof || (j == -1) || (j == 65535)) { throw new
                 * RuntimeException("Error in line "+line+ ": EOF in
                 * mid-string"); }
                 *
                 * value.append((char) j); }
                 *
                 * consume('"');
                 */
            } else if (character == '{') {
                // Value is a string enclosed in brackets. There can be pairs
                // of brackets inside of a field, so we need to count the
                // brackets to know when the string is finished.
                StringBuffer text = parseBracketedTextExactly();
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
                value.append("#").append(textToken).append("#");
            }
            skipWhitespace();
        }

        // Check if we are to strip extra pairs of braces before returning:
        if (autoDoubleBraces) {
            // Do it:
            while ((value.length() > 1) && (value.charAt(0) == '{')
                    && (value.charAt(value.length() - 1) == '}')) {
                value.deleteCharAt(value.length() - 1);
                value.deleteCharAt(0);
            }
            // Problem: if the field content is "{DNA} blahblah {EPA}", one pair
            // too much will be removed.
            // Check if this is the case, and re-add as many pairs as needed.
            while (hasNegativeBraceCount(value.toString())) {
                value.insert(0, '{');
                value.append('}');
            }
        }
        return value.toString();

    }

    /**
     * Check if a string at any point has had more ending braces (}) than
     * opening ones ({). Will e.g. return true for the string "DNA} blahblal
     * {EPA"
     *
     * @param toCheck The string to check.
     * @return true if at any index the brace count is negative.
     */
    private boolean hasNegativeBraceCount(String toCheck) {
        int index = 0;
        int braceCount = 0;
        while (index < toCheck.length()) {
            if (toCheck.charAt(index) == '{') {
                braceCount++;
            } else if (toCheck.charAt(index) == '}') {
                braceCount--;
            }
            if (braceCount < 0) {
                return true;
            }
            index++;
        }
        return false;
    }

    /**
     * This method is used to parse string labels, field names, entry type and
     * numbers outside brackets.
     */
    private String parseTextToken() throws IOException {
        StringBuilder token = new StringBuilder(20);

        while (true) {
            int character = read();
            // Util.pr(".. "+c);
            if (character == -1) {
                eof = true;

                return token.toString();
            }

            if (Character.isLetterOrDigit((char) character) || (character == ':') || (character == '-') || (character == '_')
                    || (character == '*') || (character == '+') || (character == '.') || (character == '/') || (character == '\'')) {
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
            // Util.pr(".. '"+(char)c+"'\t"+c);
            if (character == -1) {
                eof = true;

                return token.toString();
            }

            // Ikke: #{}\uFFFD~\uFFFD
            //
            // G\uFFFDr: $_*+.-\/?"^
            if (!Character.isWhitespace((char) character)
                    && (Character.isLetterOrDigit((char) character) || (character == ':') || ((character != '#') && (character != '{') && (character != '}')
                    && (character != '\uFFFD') && (character != '~') && (character != '\uFFFD') && (character != ',') && (character != '=')))) {
                token.append((char) character);
            } else {

                if (Character.isWhitespace((char) character)) {
                    // We have encountered white space instead of the comma at
                    // the end of
                    // the key. Possibly the comma is missing, so we try to
                    // return what we
                    // have found, as the key and try to restore the rest in fixKey().
                    return token + fixKey();
                } else if (character == ',') {
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

        consume('{');

        int brackets = 0;

        while (!((peek() == '}') && (brackets == 0))) {

            int character = read();
            if (isEOFCharacter(character)) {
                throw new RuntimeException("Error in line " + line + ": EOF in mid-string");
            } else if (character == '{') {
                brackets++;
            } else if (character == '}') {
                brackets--;
            }

            // If we encounter whitespace of any kind, read it as a
            // simple space, and ignore any others that follow immediately.
            /*
             * if (j == '\n') { if (peek() == '\n') value.append('\n'); } else
             */
            if (Character.isWhitespace((char) character)) {
                String whitespacesReduced = skipAndRecordWhitespace(character);

                if (!"".equals(whitespacesReduced) && !"\n\t".equals(whitespacesReduced)) { // &&
                    whitespacesReduced = whitespacesReduced.replaceAll("\t", ""); // Remove tabulators.
                    value.append(whitespacesReduced);
                } else {
                    value.append(' ');
                }

            } else {
                value.append((char) character);
            }
        }

        consume('}');

        return value;
    }

    private StringBuffer parseBracketedTextExactly() throws IOException {
        StringBuffer value = new StringBuffer();

        consume('{');

        int brackets = 0;

        while (!((peek() == '}') && (brackets == 0))) {

            int character = read();
            if (isEOFCharacter(character)) {
                throw new RuntimeException("Error in line " + line + ": EOF in mid-string");
            } else if (character == '{') {
                brackets++;
            } else if (character == '}') {
                brackets--;
            }

            value.append((char) character);
        }
        consume('}');

        return value;
    }

    private StringBuffer parseQuotedFieldExactly() throws IOException {
        StringBuffer value = new StringBuffer();

        consume('"');

        int brackets = 0;

        while (!((peek() == '"') && (brackets == 0))) {
            int j = read();
            if (isEOFCharacter(j)) {
                throw new RuntimeException("Error in line " + line + ": EOF in mid-string");
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
            throw new RuntimeException("Error in line " + line + ": Expected " + expected
                    + " but received " + (char) character);
        }
    }

    private boolean consumeUncritically(char expected) throws IOException {
        int character;
        while (((character = read()) != expected) && (character != -1) && (character != 65535)) {
            // do nothing
        }

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
            throw new RuntimeException("Error in line " + line + ": Expected " + firstOption + " or "
                    + secondOption + " but received " + character);
        }
    }

    private void checkEntryTypes(ParserResult parserResult) {
        for (BibtexEntry bibtexEntry : database.getEntries()) {
            if (bibtexEntry.getType() instanceof UnknownEntryType) {
                // Look up the unknown type name in our map of parsed types:
                String name = bibtexEntry.getType().getName();
                EntryType type = entryTypes.get(name);
                if (type != null) {
                    bibtexEntry.setType(type);
                } else {
                    parserResult.addWarning(
                            Localization.lang("Unknown entry type")
                                    + ": " + name + "; key: " + bibtexEntry.getCiteKey()
                    );
                }
            }
        }
    }

}
