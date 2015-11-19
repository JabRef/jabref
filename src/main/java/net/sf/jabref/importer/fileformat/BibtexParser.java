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
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.*;
import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.model.entry.IdGenerator;
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
        Collection<BibtexEntry> c = BibtexParser.fromString(bibtexString);
        if ((c == null) || c.isEmpty()) {
            return null;
        }
        return c.iterator().next();
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    public static boolean isRecognizedFormat(Reader inOrig) throws IOException {
        // Our strategy is to look for the "@<type>    {" line.
        BufferedReader in = new BufferedReader(inOrig);

        Pattern pat1 = Pattern.compile("@[a-zA-Z]*\\s*\\{");

        String str;

        while ((str = in.readLine()) != null) {
            if (pat1.matcher(str).find()) {
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
        database = new BibtexDatabase();
        HashMap<String, String> meta = new HashMap<>();
        entryTypes = new HashMap<>(); // To store custem entry types parsed.
        parserResult = new ParserResult(database, null, entryTypes);

        // First see if we can find the version number of the JabRef version that
        // wrote the file:
        String versionNum = readJabRefVersionNumber();
        if (versionNum != null) {
            parserResult.setJabrefVersion(versionNum);
            setMajorMinorVersions();
        }

        skipWhitespace();

        try {
            while (!eof) {
                boolean found = consumeUncritically('@');
                if (!found) {
                    break;
                }
                skipWhitespace();
                String entryType = parseTextToken();
                EntryType tp = EntryTypes.getType(entryType);
                boolean isEntry = tp != null;
                // The entry type name was not recognized. This can mean
                // that it is a string, preamble, or comment. If so,
                // parse and set accordingly. If not, assume it is an entry
                // with an unknown type.
                if (!isEntry) {
                    if (entryType.toLowerCase().equals("preamble")) {
                        database.setPreamble(parsePreamble());
                    } else if (entryType.toLowerCase().equals("string")) {
                        BibtexString bs = parseString();
                        try {
                            database.addString(bs);
                        } catch (KeyCollisionException ex) {
                            parserResult.addWarning(Localization.lang("Duplicate string name") + ": " + bs.getName());
                        }
                    } else if (entryType.toLowerCase().equals("comment")) {
                        StringBuffer commentBuf = parseBracketedTextExactly();
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
                        String comment = commentBuf.toString().replaceAll("[\\x0d\\x0a]", "");
                        if (comment.substring(0,
                                Math.min(comment.length(), GUIGlobals.META_FLAG.length())).equals(
                                GUIGlobals.META_FLAG)
                                || comment.substring(0,
                                Math.min(comment.length(), GUIGlobals.META_FLAG_OLD.length()))
                                .equals(GUIGlobals.META_FLAG_OLD)) {

                            String rest;
                            if (comment.substring(0, GUIGlobals.META_FLAG.length()).equals(
                                    GUIGlobals.META_FLAG)) {
                                rest = comment.substring(GUIGlobals.META_FLAG.length());
                            } else {
                                rest = comment.substring(GUIGlobals.META_FLAG_OLD.length());
                            }

                            int pos = rest.indexOf(':');

                            if (pos > 0) {
                                meta.put(rest.substring(0, pos), rest.substring(pos + 1));
                                // We remove all line breaks in the metadata - these
                                // will have been inserted
                                // to prevent too long lines when the file was
                                // saved, and are not part of the data.
                            }

                        } else if (comment.substring(0,
                                Math.min(comment.length(), CustomEntryType.ENTRYTYPE_FLAG.length())).equals(
                                CustomEntryType.ENTRYTYPE_FLAG)) {
                            // A custom entry type can also be stored in a
                            // "@comment"
                            CustomEntryType typ = CustomEntryTypesManager.parseEntryType(comment);
                            entryTypes.put(typ.getName().toLowerCase(), typ);
                        } else {
                            // FIXME: user comments are simply dropped
                            // at least, we log that we ignored the comment
                            LOGGER.info("Dropped comment from database: " + comment);
                        }
                    } else {
                        // The entry type was not recognized. This may mean that
                        // it is a custom entry type whose definition will
                        // appear
                        // at the bottom of the file. So we use an
                        // UnknownEntryType
                        // to remember the type name by.
                        tp = new UnknownEntryType(entryType.toLowerCase());
                        isEntry = true;
                    }
                }

                // True if not comment, preamble or string.
                if (isEntry) {
                    /**
                     * Morten Alver 13 Aug 2006: Trying to make the parser more
                     * robust. If an exception is thrown when parsing an entry,
                     * drop the entry and try to resume parsing. Add a warning
                     * for the user.
                     */
                    try {
                        BibtexEntry be = parseEntry(tp);

                        boolean duplicateKey = database.insertEntry(be);
                        if (duplicateKey) {
                            parserResult.addDuplicateKey(be.getCiteKey());
                        } else if ((be.getCiteKey() == null) || be.getCiteKey().equals("")) {
                            parserResult
                                    .addWarning(Localization.lang("Empty BibTeX key") + ": "
                                    + be.getAuthorTitleYear(40) + " ("
                                    + Localization.lang("grouping may not work for this entry") + ")");
                        }
                    } catch (IOException ex) {
                        LOGGER.warn("Could not parse entry", ex);
                        parserResult.addWarning(Localization.lang("Error occurred when parsing entry") + ": '"
                                + ex.getMessage() + "'. " + Localization.lang("Skipped entry."));

                    }
                }
                skipWhitespace();
            }
            // Before returning the database, update entries with unknown type
            // based on parsed type definitions, if possible.
            checkEntryTypes(parserResult);

            // Instantiate meta data:
            parserResult.setMetaData(new MetaData(meta, database));

            return parserResult;
        } catch (KeyCollisionException kce) {
            // kce.printStackTrace();
            throw new IOException("Duplicate ID in bibtex file: " + kce);
        }
    }

    private void skipWhitespace() throws IOException {
        int c;

        while (true) {
            c = read();
            if ((c == -1) || (c == 65535)) {
                eof = true;
                return;
            }

            if (Character.isWhitespace((char) c)) {
                continue;
            } else {
                // found non-whitespace char
                unread(c);
                break;
            }
        }
    }

    private String skipAndRecordWhitespace(int j) throws IOException {
        int c;
        StringBuilder sb = new StringBuilder();
        if (j != ' ') {
            sb.append((char) j);
        }
        while (true) {
            c = read();
            if ((c == -1) || (c == 65535)) {
                eof = true;
                return sb.toString();
            }

            if (Character.isWhitespace((char) c)) {
                if (c != ' ') {
                    sb.append((char) c);
                }
                continue;
            } else {
                // found non-whitespace char
                unread(c);
                break;
            }
        }
        return sb.toString();
    }

    private int peek() throws IOException {
        int c = read();
        unread(c);

        return c;
    }

    private int read() throws IOException {
        int c = pushbackReader.read();
        if (c == '\n') {
            line++;
        }
        return c;
    }

    private void unread(int c) throws IOException {
        if (c == '\n') {
            line--;
        }
        pushbackReader.unread(c);
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

    private BibtexEntry parseEntry(EntryType tp) throws IOException {
        String id = IdGenerator.next();
        BibtexEntry result = new BibtexEntry(id, tp);
        skipWhitespace();
        consume('{', '(');
        int c = peek();
        if ((c != '\n') && (c != '\r')) {
            skipWhitespace();
        }
        String key = parseKey();

        if ((key != null) && key.equals("")) {
            key = null;
        }

        result.setField(BibtexEntry.KEY_FIELD, key);
        skipWhitespace();

        while (true) {
            c = peek();
            if ((c == '}') || (c == ')')) {
                break;
            }

            if (c == ',') {
                consume(',');
            }

            skipWhitespace();

            c = peek();
            if ((c == '}') || (c == ')')) {
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
                if (key.equals("author") || key.equals("editor")) {
                    entry.setField(key, entry.getField(key) + " and " + content);
                }
            }
        }
    }

    private String parseFieldContent(String key) throws IOException {
        skipWhitespace();
        StringBuilder value = new StringBuilder();
        int c;

        while (((c = peek()) != ',') && (c != '}') && (c != ')')) {

            if (eof) {
                throw new RuntimeException("Error in line " + line + ": EOF in mid-string");
            }
            if (c == '"') {
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
            } else if (c == '{') {
                // Value is a string enclosed in brackets. There can be pairs
                // of brackets inside of a field, so we need to count the
                // brackets to know when the string is finished.
                StringBuffer text = parseBracketedTextExactly();
                value.append(fieldContentParser.format(text, key));

            } else if (Character.isDigit((char) c)) { // value is a number
                String numString = parseTextToken();
                value.append(numString);
            } else if (c == '#') {
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
     * @param s The string to check.
     * @return true if at any index the brace count is negative.
     */
    private boolean hasNegativeBraceCount(String s) {
        int i = 0;
        int count = 0;
        while (i < s.length()) {
            if (s.charAt(i) == '{') {
                count++;
            } else if (s.charAt(i) == '}') {
                count--;
            }
            if (count < 0) {
                return true;
            }
            i++;
        }
        return false;
    }

    /**
     * This method is used to parse string labels, field names, entry type and
     * numbers outside brackets.
     */
    private String parseTextToken() throws IOException {
        // TODO: why default capacity of 20?
        StringBuilder token = new StringBuilder(20);

        while (true) {
            int c = read();
            // Util.pr(".. "+c);
            if (c == -1) {
                eof = true;

                return token.toString();
            }

            if (Character.isLetterOrDigit((char) c) || (c == ':') || (c == '-') || (c == '_')
                    || (c == '*') || (c == '+') || (c == '.') || (c == '/') || (c == '\'')) {
                token.append((char) c);
            } else {
                unread(c);
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
        int lookahead_used = 0;
        char currentChar;

        // Find a char which ends key (','&&'\n') or entryfield ('='):
        do {
            currentChar = (char) read();
            key.append(currentChar);
            lookahead_used++;
        } while ((currentChar != ',') && (currentChar != '\n') && (currentChar != '=')
                && (lookahead_used < BibtexParser.LOOKAHEAD));

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
     * removes whitespaces from <code>sb</code>
     *
     * @param sb
     * @return
     */
    private StringBuilder removeWhitespaces(StringBuilder sb) {
        StringBuilder newSb = new StringBuilder();
        char current;
        for (int i = 0; i < sb.length(); ++i) {
            current = sb.charAt(i);
            if (!Character.isWhitespace(current)) {
                newSb.append(current);
            }
        }
        return newSb;
    }

    /**
     * pushes buffer back into input
     *
     * @param sb
     * @throws IOException can be thrown if buffer is bigger than LOOKAHEAD
     */
    private void unreadBuffer(StringBuilder sb) throws IOException {
        for (int i = sb.length() - 1; i >= 0; --i) {
            unread(sb.charAt(i));
        }
    }

    /**
     * This method is used to parse the bibtex key for an entry.
     */
    private String parseKey() throws IOException {
        StringBuilder token = new StringBuilder(20);

        while (true) {
            int c = read();
            // Util.pr(".. '"+(char)c+"'\t"+c);
            if (c == -1) {
                eof = true;

                return token.toString();
            }

            // Ikke: #{}\uFFFD~\uFFFD
            //
            // G\uFFFDr: $_*+.-\/?"^
            if (!Character.isWhitespace((char) c)
                    && (Character.isLetterOrDigit((char) c) || (c == ':') || ((c != '#') && (c != '{') && (c != '}')
                    && (c != '\uFFFD') && (c != '~') && (c != '\uFFFD') && (c != ',') && (c != '=')))) {
                token.append((char) c);
            } else {

                if (Character.isWhitespace((char) c)) {
                    // We have encountered white space instead of the comma at
                    // the end of
                    // the key. Possibly the comma is missing, so we try to
                    // return what we
                    // have found, as the key and try to restore the rest in fixKey().
                    return token + fixKey();
                } else if (c == ',') {
                    unread(c);
                    return token.toString();
                } else if (c == '=') {
                    // If we find a '=' sign, it is either an error, or
                    // the entry lacked a comma signifying the end of the key.
                    return token.toString();
                } else {
                    throw new IOException("Error in line " + line + ":" + "Character '" + (char) c
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

            int j = read();
            if ((j == -1) || (j == 65535)) {
                throw new RuntimeException("Error in line " + line + ": EOF in mid-string");
            } else if (j == '{') {
                brackets++;
            } else if (j == '}') {
                brackets--;
            }

            // If we encounter whitespace of any kind, read it as a
            // simple space, and ignore any others that follow immediately.
            /*
             * if (j == '\n') { if (peek() == '\n') value.append('\n'); } else
             */
            if (Character.isWhitespace((char) j)) {
                String whs = skipAndRecordWhitespace(j);

                if (!whs.equals("") && !whs.equals("\n\t")) { // &&
                    whs = whs.replaceAll("\t", ""); // Remove tabulators.
                    value.append(whs);
                } else {
                    value.append(' ');
                }

            } else {
                value.append((char) j);
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

            int j = read();
            if ((j == -1) || (j == 65535)) {
                throw new RuntimeException("Error in line " + line + ": EOF in mid-string");
            } else if (j == '{') {
                brackets++;
            } else if (j == '}') {
                brackets--;
            }

            value.append((char) j);
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
            if ((j == -1) || (j == 65535)) {
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
        int c = read();

        if (c != expected) {
            throw new RuntimeException("Error in line " + line + ": Expected " + expected
                    + " but received " + (char) c);
        }
    }

    private boolean consumeUncritically(char expected) throws IOException {
        int c;
        while (((c = read()) != expected) && (c != -1) && (c != 65535)) {
            // do nothing
        }

        if ((c == -1) || (c == 65535)) {
            eof = true;
        }

        // Return true if we actually found the character we were looking for:
        return c == expected;
    }

    private void consume(char expected1, char expected2) throws IOException {
        // Consumes one of the two, doesn't care which appears.

        int c = read();

        if ((c != expected1) && (c != expected2)) {
            throw new RuntimeException("Error in line " + line + ": Expected " + expected1 + " or "
                    + expected2 + " but received " + c);
        }
    }

    private void checkEntryTypes(ParserResult _pr) {
        for (BibtexEntry be : database.getEntries()) {
            if (be.getType() instanceof UnknownEntryType) {
                // Look up the unknown type name in our map of parsed types:
                String name = be.getType().getName();
                EntryType type = entryTypes.get(name.toLowerCase());
                if (type != null) {
                    be.setType(type);
                } else {
                    _pr.addWarning(
                            Localization.lang("Unknown entry type")
                                    + ": " + name + "; key: " + be.getCiteKey()
                            );

                    be.setType(new BibtexEntryType() {
                        @Override
                        public String getName() {
                            return name;
                        }
                    });
                }
            }
        }
    }

    /**
     * Read the JabRef signature, if any, and find what version number is given.
     * This method advances the file reader only as far as the end of the first line of
     * the JabRef signature, or up until the point where the read characters don't match
     * the signature. This should ensure that the parser can continue from that spot without
     * resetting the reader, without the risk of losing important contents.
     *
     * @return The version number, or null if not found.
     * @throws IOException
     */
    private String readJabRefVersionNumber() throws IOException {
        StringBuilder headerText = new StringBuilder();

        boolean keepon = true;
        int piv = 0;
        int c;

        // We start by reading the standard part of the signature, which precedes
        // the version number: This file was created with JabRef X.y.
        while (keepon) {
            c = peek();
            headerText.append((char) c);
            if ((piv == 0) && (Character.isWhitespace((char) c) || (c == '%'))) {
                read();
            } else if (c == Globals.SIGNATURE.charAt(piv)) {
                piv++;
                read();
            } else {
                return null;
            }

            // Check if we've reached the end of the signature's standard part:
            if (piv == Globals.SIGNATURE.length()) {
                keepon = false;

                // Found the standard part. Now read the version number:
                StringBuilder sb = new StringBuilder();
                while (((c = read()) != '\n') && (c != -1)) {
                    sb.append((char) c);
                }
                String versionNum = sb.toString().trim();
                // See if it fits the X.y. pattern:
                if (Pattern.compile("[1-9]+\\.[1-9A-Za-z ]+\\.").matcher(versionNum).matches()) {
                    // It matched. Remove the last period and return:
                    return versionNum.substring(0, versionNum.length() - 1);
                } else if (Pattern.compile("[1-9]+\\.[1-9]\\.[1-9A-Za-z ]+\\.").matcher(versionNum).matches()) {
                    // It matched. Remove the last period and return:
                    return versionNum.substring(0, versionNum.length() - 1);
                }

            }
        }
        return null;
    }

    /**
     * After a JabRef version number has been parsed and put into _pr,
     * parse the version number to determine the JabRef major and minor version
     * number
     */
    private void setMajorMinorVersions() {
        String v = parserResult.getJabrefVersion();
        Pattern p = Pattern.compile("([0-9]+)\\.([0-9]+).*");
        Matcher m = p.matcher(v);
        if (m.matches()) {
            if (m.groupCount() >= 2) {
                parserResult.setJabrefMajorVersion(Integer.parseInt(m.group(1)));
                parserResult.setJabrefMinorVersion(Integer.parseInt(m.group(2)));
            }
        }
    }
}
