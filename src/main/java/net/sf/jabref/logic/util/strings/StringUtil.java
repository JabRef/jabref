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
package net.sf.jabref.logic.util.strings;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.l10n.Encodings;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    // contains all possible line breaks, not ommitting any break such as "\\n"
    private static final Pattern LINE_BREAKS = Pattern.compile("\\r\\n|\\r|\\n");

    /**
     * Returns the string, after shaving off whitespace at the beginning and end,
     * and removing (at most) one pair of braces or " surrounding it.
     *
     * @param toShave
     * @return
     */
    public static String shaveString(String toShave) {

        if (toShave == null) {
            return null;
        }
        char first;
        char second;
        int begin = 0;
        int end = toShave.length();
        // We start out assuming nothing will be removed.
        boolean beginOk = false;
        boolean endOk = false;
        while (!beginOk) {
            if (begin < toShave.length()) {
                first = toShave.charAt(begin);
                if (Character.isWhitespace(first)) {
                    begin++;
                } else {
                    beginOk = true;
                }
            } else {
                beginOk = true;
            }

        }
        while (!endOk) {
            if (end > (begin + 1)) {
                first = toShave.charAt(end - 1);
                if (Character.isWhitespace(first)) {
                    end--;
                } else {
                    endOk = true;
                }
            } else {
                endOk = true;
            }
        }

        if (end > (begin + 1)) {
            first = toShave.charAt(begin);
            second = toShave.charAt(end - 1);
            if (((first == '{') && (second == '}')) || ((first == '"') && (second == '"'))) {
                begin++;
                end--;
            }
        }
        toShave = toShave.substring(begin, end);
        return toShave;
    }

    private static String rightTrim(String toTrim) {
        return toTrim.replaceAll("\\s+$", "");
    }

    /**
     * Concatenate all strings in the array from index 'from' to 'to' (excluding
     * to) with the given separator.
     * <p>
     * Example:
     * <p>
     * String[] s = "ab/cd/ed".split("/"); join(s, "\\", 0, s.length) ->
     * "ab\\cd\\ed"
     *
     * @param strings
     * @param separator
     * @param from
     * @param to        Excluding strings[to]
     * @return
     */
    public static String join(String[] strings, String separator, int from, int to) {
        if ((strings.length == 0) || (from >= to)) {
            return "";
        }

        from = Math.max(from, 0);
        to = Math.min(strings.length, to);

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = from; i < (to - 1); i++) {
            stringBuilder.append(strings[i]).append(separator);
        }
        return stringBuilder.append(strings[to - 1]).toString();
    }

    public static String join(String[] strings, String separator) {
        return join(strings, separator, 0, strings.length);
    }

    /**
     * Returns the given string but with the first character turned into an
     * upper case character.
     * <p>
     * Example: testTest becomes TestTest
     *
     * @param string The string to change the first character to upper case to.
     * @return A string has the first character turned to upper case and the
     * rest unchanged from the given one.
     */
    public static String toUpperFirstLetter(String string) {
        if (string == null) {
            throw new IllegalArgumentException();
        }

        if (string.isEmpty()) {
            return string;
        }

        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    /**
     * Takes a delimited string, splits it and returns
     *
     * @param names a <code>String</code> value
     * @return a <code>String[]</code> value
     */
    public static String[] split(String names, String delimiter) {
        if (names == null) {
            return null;
        }
        return names.split(delimiter);
    }

    public static String capitalizeFirst(String toCapitalize) {
        // Make first character of String uppercase, and the
        // rest lowercase.
        if (toCapitalize.length() > 1) {
            return toCapitalize.substring(0, 1).toUpperCase() + toCapitalize.substring(1, toCapitalize.length()).toLowerCase();
        } else {
            return toCapitalize.toUpperCase();
        }

    }

    /**
     * Removes optional square brackets from the string s
     *
     * @param toStrip
     * @return
     */
    public static String stripBrackets(String toStrip) {
        int beginIndex = toStrip.startsWith("[") ? 1 : 0;
        int endIndex = toStrip.endsWith("]") ? toStrip.length() - 1 : toStrip.length();
        return toStrip.substring(beginIndex, endIndex);
    }

    /**
     * extends the filename with a default Extension, if no Extension '.x' could
     * be found
     */
    public static String getCorrectFileName(String orgName, String defaultExtension) {
        if (orgName == null) {
            return "";
        }

        String back = orgName;
        int hiddenChar = orgName.indexOf(".", 1); // hidden files Linux/Unix (?)
        if (hiddenChar < 1) {
            back = back + "." + defaultExtension;
        }

        return back;
    }

    /**
     * Creates a substring from a text
     *
     * @param text
     * @param index
     * @param terminateOnEndBraceOnly
     * @return
     */
    public static String getPart(String text, int index, boolean terminateOnEndBraceOnly) {
        char c;
        int count = 0;

        StringBuilder part = new StringBuilder();

        // advance to first char and skip whitespace
        index++;
        while ((index < text.length()) && Character.isWhitespace(text.charAt(index))) {
            index++;
        }

        // then grab whathever is the first token (counting braces)
        while (index < text.length()) {
            c = text.charAt(index);
            if (!terminateOnEndBraceOnly && (count == 0) && Character.isWhitespace(c)) {
                // end argument and leave whitespace for further processing
                break;
            }
            if ((c == '}') && (--count < 0)) {
                break;
            } else if (c == '{') {
                count++;
            }
            part.append(c);
            index++;
        }
        return part.toString();
    }

    /**
     * Formats field contents for output. Must be "symmetric" with the parse method above,
     * so stored and reloaded fields are not mangled.
     *
     * @param in
     * @param wrapAmount
     * @return the wrapped String.
     */
    public static String wrap(String in, int wrapAmount) {

        String[] lines = in.split("\n");
        StringBuilder result = new StringBuilder();
        addWrappedLine(result, lines[0], wrapAmount);
        for (int i = 1; i < lines.length; i++) {

            if (!lines[i].trim().equals("")) {
                result.append(Globals.NEWLINE);
                result.append('\t');
                result.append(Globals.NEWLINE);
                result.append('\t');
                String line = lines[i];
                // remove all whitespace at the end of the string, this especially includes \r created when the field content has \r\n as line separator
                line = rightTrim(line);
                addWrappedLine(result, line, wrapAmount);
            } else {
                result.append(Globals.NEWLINE);
                result.append('\t');
            }
        }
        return result.toString();
    }

    private static void addWrappedLine(StringBuilder result, String line, int wrapAmount) {
        // Set our pointer to the beginning of the new line in the StringBuffer:
        int length = result.length();
        // Add the line, unmodified:
        result.append(line);

        while (length < result.length()) {
            int current = result.indexOf(" ", length + wrapAmount);
            if ((current < 0) || (current >= result.length())) {
                break;
            }

            result.deleteCharAt(current);
            result.insert(current, Globals.NEWLINE + "\t");
            length = current + Globals.NEWLINE.length();

        }
    }

    /**
     * Quotes each and every character, e.g. '!' as &#33;. Used for verbatim
     * display of arbitrary strings that may contain HTML entities.
     */
    public static String quoteForHTML(String toQuote) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < toQuote.length(); ++i) {
            result.append("&#").append((int) toQuote.charAt(i)).append(";");
        }
        return result.toString();
    }

    public static String quote(String toQuote, String specials, char quoteChar) {
        return quote(toQuote, specials, quoteChar, 0);
    }

    /**
     * Quote special characters.
     *
     * @param toQuote         The String which may contain special characters.
     * @param specials  A String containing all special characters except the quoting
     *                  character itself, which is automatically quoted.
     * @param quoteChar The quoting character.
     * @param linewrap  The number of characters after which a linebreak is inserted
     *                  (this linebreak is undone by unquote()). Set to 0 to disable.
     * @return A String with every special character (including the quoting
     * character itself) quoted.
     */
    private static String quote(String toQuote, String specials, char quoteChar, int linewrap) {
        StringBuilder result = new StringBuilder();
        char c;
        int lineLength = 0;
        boolean isSpecial;
        for (int i = 0; i < toQuote.length(); ++i) {
            c = toQuote.charAt(i);
            isSpecial = (specials.indexOf(c) >= 0) || (c == quoteChar);
            // linebreak?
            if ((linewrap > 0) && ((++lineLength >= linewrap) || (isSpecial && (lineLength >= (linewrap - 1))))) {
                result.append(quoteChar);
                result.append('\n');
                lineLength = 0;
            }
            if (isSpecial) {
                result.append(quoteChar);
                ++lineLength;
            }
            result.append(c);
        }
        return result.toString();
    }

    /**
     * Unquote special characters.
     *
     * @param toUnquote         The String which may contain quoted special characters.
     * @param quoteChar The quoting character.
     * @return A String with all quoted characters unquoted.
     */
    public static String unquote(String toUnquote, char quoteChar) {
        StringBuilder result = new StringBuilder();
        char c;
        boolean quoted = false;
        for (int i = 0; i < toUnquote.length(); ++i) {
            c = toUnquote.charAt(i);
            if (quoted) { // append literally...
                if (c != '\n') {
                    result.append(c);
                }
                quoted = false;
            } else if (c != quoteChar) {
                result.append(c);
            } else { // quote char
                quoted = true;
            }
        }
        return result.toString();
    }

    /**
     * Append '.bib' to the string unless it ends with that.
     * <p>
     * makeBibtexExtension("asfd") => "asdf.bib"
     * makeBibtexExtension("asdf.bib") => "asdf.bib"
     *
     * @param name the string
     * @return s or s + ".bib"
     */
    public static String makeBibtexExtension(String name) {
        if (!name.toLowerCase().endsWith(".bib")) {
            return name + ".bib";
        }
        return name;
    }

    public static String booleanToBinaryString(boolean expression) {
        return expression ? "1" : "0";
    }

    /**
     * Make a list of supported character encodings that can encode all
     * characters in the given String.
     *
     * @param characters
     *            A String of characters that should be supported by the
     *            encodings.
     * @return A List of character encodings
     */
    public static List<String> findEncodingsForString(String characters) {
        List<String> encodings = new ArrayList<>();
        for (int i = 0; i < Encodings.ENCODINGS.length; i++) {
            CharsetEncoder encoder = Charset.forName(Encodings.ENCODINGS[i]).newEncoder();
            if (encoder.canEncode(characters)) {
                encodings.add(Encodings.ENCODINGS[i]);
            }
        }
        return encodings;
    }

    /**
     * Encodes a two-dimensional String array into a single string, using ':' and
     * ';' as separators. The characters ':' and ';' are escaped with '\'.
     * @param values The String array.
     * @return The encoded String.
     */
    public static String encodeStringArray(String[][] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            sb.append(encodeStringArray(values[i]));
            if (i < (values.length - 1)) {
                sb.append(';');
            }
        }
        return sb.toString();
    }

    /**
     * Encodes a String array into a single string, using ':' as separator.
     * The characters ':' and ';' are escaped with '\'.
     * @param entry The String array.
     * @return The encoded String.
     */
    private static String encodeStringArray(String[] entry) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entry.length; i++) {
            sb.append(encodeString(entry[i]));
            if (i < (entry.length - 1)) {
                sb.append(':');
            }

        }
        return sb.toString();
    }

    /**
     * Decodes an encoded double String array back into array form. The array
     * is assumed to be square, and delimited by the characters ';' (first dim) and
     * ':' (second dim).
     * @param value The encoded String to be decoded.
     * @return The decoded String array.
     */
    public static String[][] decodeStringDoubleArray(String value) {
        ArrayList<ArrayList<String>> newList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        ArrayList<String> thisEntry = new ArrayList<>();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!escaped && (c == '\\')) {
                escaped = true;
                continue;
            }
            else if (!escaped && (c == ':')) {
                thisEntry.add(sb.toString());
                sb = new StringBuilder();
            }
            else if (!escaped && (c == ';')) {
                thisEntry.add(sb.toString());
                sb = new StringBuilder();
                newList.add(thisEntry);
                thisEntry = new ArrayList<>();
            } else {
                sb.append(c);
            }
            escaped = false;
        }
        if (sb.length() > 0) {
            thisEntry.add(sb.toString());
        }
        if (!thisEntry.isEmpty()) {
            newList.add(thisEntry);
        }

        // Convert to String[][]:
        String[][] res = new String[newList.size()][];
        for (int i = 0; i < res.length; i++) {
            res[i] = new String[newList.get(i).size()];
            for (int j = 0; j < res[i].length; j++) {
                res[i][j] = newList.get(i).get(j);
            }
        }
        return res;
    }

    public static String encodeString(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == ';') || (c == ':') || (c == '\\')) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Build a String array containing all those elements of all that are not
     * in subset.
     * @param all The array of all values.
     * @param subset The subset of values.
     * @return The remainder that is not part of the subset.
     */
    public static String[] getRemainder(String[] all, String[] subset) {
    	if (subset.length == 0) {
    		return all;
    	}
    	if (all.equals(subset)) {
    		return new String[0];
    	}

        ArrayList<String> al = new ArrayList<>();
        for (String anAll : all) {
            boolean found = false;
            for (String aSubset : subset) {
                if (aSubset.equals(anAll)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                al.add(anAll);
            }
        }
        return al.toArray(new String[al.size()]);
    }

    /**
	 * Concatenate two String arrays
	 *
	 * @param array1
	 *            the first string array
	 * @param array2
	 *            the second string array
	 * @return The concatenation of array1 and array2
	 */
	public static String[] arrayConcat(String[] array1, String[] array2) {
		int len1 = array1.length;
		int len2 = array2.length;
		String[] union = new String[len1 + len2];
		System.arraycopy(array1, 0, union, 0, len1);
		System.arraycopy(array2, 0, union, len1, len2);
		return union;
	}

    /**
     * Wrap all uppercase letters, or sequences of uppercase letters, in curly
     * braces. Ignore letters within a pair of # character, as these are part of
     * a string label that should not be modified.
     *
     * @param s
     *            The string to modify.
     * @return The resulting string after wrapping capitals.
     */
    public static String putBracesAroundCapitals(String s) {

        boolean inString = false;
        boolean isBracing = false;
        boolean escaped = false;
        int inBrace = 0;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            // Update variables based on special characters:
            int c = s.charAt(i);
            if (c == '{') {
                inBrace++;
            } else if (c == '}') {
                inBrace--;
            } else if (!escaped && (c == '#')) {
                inString = !inString;
            }

            // See if we should start bracing:
            if ((inBrace == 0) && !isBracing && !inString && Character.isLetter((char) c)
                    && Character.isUpperCase((char) c)) {

                buf.append('{');
                isBracing = true;
            }

            // See if we should close a brace set:
            if (isBracing && !(Character.isLetter((char) c) && Character.isUpperCase((char) c))) {

                buf.append('}');
                isBracing = false;
            }

            // Add the current character:
            buf.append((char) c);

            // Check if we are entering an escape sequence:
            escaped = (c == '\\') && !escaped;

        }
        // Check if we have an unclosed brace:
        if (isBracing) {
            buf.append('}');
        }

        return buf.toString();

        /*
         * if (s.length() == 0) return s; // Protect against ArrayIndexOutOf....
         * StringBuffer buf = new StringBuffer();
         *
         * Matcher mcr = titleCapitalPattern.matcher(s.substring(1)); while
         * (mcr.find()) { String replaceStr = mcr.group();
         * mcr.appendReplacement(buf, "{" + replaceStr + "}"); }
         * mcr.appendTail(buf); return s.substring(0, 1) + buf.toString();
         */
    }

    /**
     * This method looks for occurrences of capital letters enclosed in an
     * arbitrary number of pairs of braces, e.g. "{AB}" or "{{T}}". All of these
     * pairs of braces are removed.
     *
     * @param s
     *            The String to analyze.
     * @return A new String with braces removed.
     */
    public static String removeBracesAroundCapitals(String s) {
        String previous = s;
        while ((s = removeSingleBracesAroundCapitals(s)).length() < previous.length()) {
            previous = s;
        }
        return s;
    }

    /**
     * This method looks for occurrences of capital letters enclosed in one pair
     * of braces, e.g. "{AB}". All these are replaced by only the capitals in
     * between the braces.
     *
     * @param s
     *            The String to analyze.
     * @return A new String with braces removed.
     */
    private static String removeSingleBracesAroundCapitals(String s) {
        final Pattern BRACED_TITLE_CAPITAL_PATTERN = Pattern.compile("\\{[A-Z]+\\}");

        Matcher mcr = BRACED_TITLE_CAPITAL_PATTERN.matcher(s);
        StringBuffer buf = new StringBuffer();
        while (mcr.find()) {
            String replaceStr = mcr.group();
            mcr.appendReplacement(buf, replaceStr.substring(1, replaceStr.length() - 1));
        }
        mcr.appendTail(buf);
        return buf.toString();
    }

    /**
     * Replaces all platform-dependent line breaks by Globals.NEWLINE line breaks.
     *
     * We do NOT use UNIX line breaks as the user explicitly configures its linebreaks and this method is used in bibtex field writing
     *
     * <example>
     * Legacy Macintosh \r -> Globals.NEWLINE
     * Windows \r\n -> Globals.NEWLINE
     * </example>
     *
     * @return a String with only Globals.NEWLINE as line breaks
     */
    public static String unifyLineBreaksToConfiguredLineBreaks(String s) {
        return LINE_BREAKS.matcher(s).replaceAll(Globals.NEWLINE);
    }
}
