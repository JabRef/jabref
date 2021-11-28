package org.jabref.model.strings;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.architecture.ApacheCommonsLang3Allowed;

import com.google.common.base.CharMatcher;
import org.apache.commons.lang3.StringUtils;

@ApacheCommonsLang3Allowed("There is no equivalent in Google's Guava")
public class StringUtil {

    // Non-letters which are used to denote accents in LaTeX-commands, e.g., in {\"{a}}
    public static final String SPECIAL_COMMAND_CHARS = "\"`^~'=.|";
    // contains all possible line breaks, not omitting any break such as "\\n"
    private static final Pattern LINE_BREAKS = Pattern.compile("\\r\\n|\\r|\\n");
    private static final Pattern BRACED_TITLE_CAPITAL_PATTERN = Pattern.compile("\\{[A-Z]+\\}");
    private static final UnicodeToReadableCharMap UNICODE_CHAR_MAP = new UnicodeToReadableCharMap();

    public static String booleanToBinaryString(boolean expression) {
        return expression ? "1" : "0";
    }

    /**
     * Quote special characters.
     *
     * @param toQuote   The String which may contain special characters.
     * @param specials  A String containing all special characters except the quoting character itself, which is automatically quoted.
     * @param quoteChar The quoting character.
     * @return A String with every special character (including the quoting character itself) quoted.
     */
    public static String quote(String toQuote, String specials, char quoteChar) {
        if (toQuote == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        char c;
        boolean isSpecial;
        for (int i = 0; i < toQuote.length(); ++i) {
            c = toQuote.charAt(i);

            isSpecial = (c == quoteChar);
            // If non-null specials performs logic-or with specials.indexOf(c) >= 0
            isSpecial |= ((specials != null) && (specials.indexOf(c) >= 0));

            if (isSpecial) {
                result.append(quoteChar);
            }
            result.append(c);
        }
        return result.toString();
    }

    /**
     * Creates a substring from a text
     *
     * @param text
     * @param startIndex
     * @param terminateOnEndBraceOnly
     * @return
     */
    public static String getPart(String text, int startIndex, boolean terminateOnEndBraceOnly) {
        char c;
        int count = 0;

        StringBuilder part = new StringBuilder();

        // advance to first char and skip whitespace
        int index = startIndex + 1;
        while ((index < text.length()) && Character.isWhitespace(text.charAt(index))) {
            index++;
        }

        // then grab whatever is the first token (counting braces)
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
     * Returns the string, after shaving off whitespace at the beginning and end,
     * and removing (at most) one pair of braces or " surrounding it.
     *
     * @param toShave
     * @return
     */

    public static String shaveString(String toShave) {
        if ((toShave == null) || (toShave.isEmpty())) {
            return "";
        }
        String shaved = toShave.trim();
        if (isInCurlyBrackets(shaved) || isInCitationMarks(shaved)) {
            return shaved.substring(1, shaved.length() - 1);
        }
        return shaved;
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

        int updatedFrom = Math.max(0, from);
        int updatedTo = Math.min(strings.length, to);

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = updatedFrom; i < (updatedTo - 1); i++) {
            stringBuilder.append(strings[i]).append(separator);
        }
        return stringBuilder.append(strings[updatedTo - 1]).toString();
    }

    /**
     * Removes optional square brackets from the string s
     *
     * @param toStrip
     * @return
     */
    public static String stripBrackets(String toStrip) {
        if (isInSquareBrackets(toStrip)) {
            return toStrip.substring(1, toStrip.length() - 1);
        }
        return toStrip;
    }

    /**
     * extends the filename with a default Extension, if no Extension '.x' could
     * be found
     */
    public static String getCorrectFileName(String orgName, String defaultExtension) {
        if (orgName == null) {
            return "";
        }

        if (orgName.toLowerCase(Locale.ROOT).endsWith("." + defaultExtension.toLowerCase(Locale.ROOT))) {
            return orgName;
        }

        int hiddenChar = orgName.indexOf('.', 1); // hidden files Linux/Unix (?)
        if (hiddenChar < 1) {
            return orgName + "." + defaultExtension;
        }

        return orgName;
    }

    /**
     * Formats field contents for output. Must be "symmetric" with the parse method above, so stored and reloaded fields
     * are not mangled.
     *
     * @param in         the string to wrap
     * @param wrapAmount the number of characters belonging to a line of text
     * @param newline    the newline character(s)
     * @return the wrapped string
     */
    public static String wrap(String in, int wrapAmount, String newline) {
        String[] lines = in.split("\n");
        StringBuilder result = new StringBuilder();
        // remove all whitespace at the end of the string, this especially includes \r created when the field content has \r\n as line separator
        addWrappedLine(result, CharMatcher.whitespace().trimTrailingFrom(lines[0]), wrapAmount, newline);
        for (int i = 1; i < lines.length; i++) {

            if (lines[i].trim().isEmpty()) {
                result.append(newline);
                result.append('\t');
            } else {
                result.append(newline);
                result.append('\t');
                result.append(newline);
                result.append('\t');
                // remove all whitespace at the end of the string, this especially includes \r created when the field content has \r\n as line separator
                String line = CharMatcher.whitespace().trimTrailingFrom(lines[i]);
                addWrappedLine(result, line, wrapAmount, newline);
            }
        }
        return result.toString();
    }

    /**
     * Appends a text to a string builder. Wraps the text so that each line is approx wrapAmount characters long.
     * Wrapping is done using newline and tab character.
     *
     * @param line          the line of text to be wrapped and appended
     * @param wrapAmount    the number of characters belonging to a line of text
     * @param newlineString a string containing the newline character(s)
     */
    private static void addWrappedLine(StringBuilder result, String line, int wrapAmount, String newlineString) {
        // Set our pointer to the beginning of the new line in the StringBuffer:
        int length = result.length();
        // Add the line, unmodified:
        result.append(line);

        // insert newlines and one tab character at each position, where wrapping is necessary
        while (length < result.length()) {
            int current = result.indexOf(" ", length + wrapAmount);
            if ((current < 0) || (current >= result.length())) {
                break;
            }

            result.deleteCharAt(current);
            result.insert(current, newlineString + "\t");
            length = current + newlineString.length();
        }
    }

    /**
     * Quotes each and every character, e.g. '!' as &#33;. Used for verbatim
     * display of arbitrary strings that may contain HTML entities.
     */
    public static String quoteForHTML(String toQuote) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < toQuote.length(); ++i) {
            result.append("&#").append((int) toQuote.charAt(i)).append(';');
        }
        return result.toString();
    }

    /**
     * Decodes an encoded double String array back into array form. The array
     * is assumed to be square, and delimited by the characters ';' (first dim) and
     * ':' (second dim).
     * @param value The encoded String to be decoded.
     * @return The decoded String array.
     */
    public static String[][] decodeStringDoubleArray(String value) {
        List<List<String>> newList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        List<String> thisEntry = new ArrayList<>();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!escaped && (c == '\\')) {
                escaped = true;
                continue;
            } else if (!escaped && (c == ':')) {
                thisEntry.add(sb.toString());
                sb = new StringBuilder();
            } else if (!escaped && (c == ';')) {
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

    /**
     * Wrap all uppercase letters, or sequences of uppercase letters, in curly
     * braces. Ignore letters within a pair of # character, as these are part of
     * a string label that should not be modified.
     *
     * @param s The string to modify.
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
    }

    /**
     * This method looks for occurrences of capital letters enclosed in an
     * arbitrary number of pairs of braces, e.g. "{AB}" or "{{T}}". All of these
     * pairs of braces are removed.
     *
     * @param s The String to analyze.
     * @return A new String with braces removed.
     */
    public static String removeBracesAroundCapitals(String s) {
        String current = s;
        String previous = s;
        while ((current = removeSingleBracesAroundCapitals(current)).length() < previous.length()) {
            previous = current;
        }
        return current;
    }

    /**
     * This method looks for occurrences of capital letters enclosed in one pair
     * of braces, e.g. "{AB}". All these are replaced by only the capitals in
     * between the braces.
     *
     * @param s The String to analyze.
     * @return A new String with braces removed.
     */
    private static String removeSingleBracesAroundCapitals(String s) {
        Matcher mcr = BRACED_TITLE_CAPITAL_PATTERN.matcher(s);
        StringBuilder buf = new StringBuilder();
        while (mcr.find()) {
            String replaceStr = mcr.group();
            mcr.appendReplacement(buf, replaceStr.substring(1, replaceStr.length() - 1));
        }
        mcr.appendTail(buf);
        return buf.toString();
    }

    /**
     * Replaces all platform-dependent line breaks by OS.NEWLINE line breaks.
     * AKA normalize newlines
     * <p>
     * We do NOT use UNIX line breaks as the user explicitly configures its linebreaks and this method is used in bibtex field writing
     *
     * <example>
     * Legacy Macintosh \r -> OS.NEWLINE
     * Windows \r\n -> OS.NEWLINE
     * </example>
     *
     * @return a String with only OS.NEWLINE as line breaks
     */
    public static String unifyLineBreaks(String s, String newline) {
        return LINE_BREAKS.matcher(s).replaceAll(newline);
    }

    /**
     * Checks if the given String has exactly one pair of surrounding curly braces <br>
     * Strings with escaped characters in curly braces at the beginning and end are respected, too
     * @param toCheck The string to check
     * @return True, if the check was succesful. False otherwise.
     */
    public static boolean isInCurlyBrackets(String toCheck) {
        int count = 0;
        int brackets = 0;
        if ((toCheck == null) || toCheck.isEmpty()) {
            return false;
        } else {
            if ((toCheck.charAt(0) == '{') && (toCheck.charAt(toCheck.length() - 1) == '}')) {
                for (char c : toCheck.toCharArray()) {
                    if (c == '{') {
                        if (brackets == 0) {
                            count++;
                        }
                        brackets++;
                    } else if (c == '}') {
                        brackets--;
                    }
                }

                return count == 1;
            }
            return false;
        }
    }

    public static boolean isInSquareBrackets(String toCheck) {
        if ((toCheck == null) || toCheck.isEmpty()) {
            return false; // In case of null or empty string
        } else {
            return (toCheck.charAt(0) == '[') && (toCheck.charAt(toCheck.length() - 1) == ']');
        }
    }

    public static boolean isInCitationMarks(String toCheck) {
        if ((toCheck == null) || (toCheck.length() <= 1)) {
            return false; // In case of null, empty string, or a single citation mark
        } else {
            return (toCheck.charAt(0) == '"') && (toCheck.charAt(toCheck.length() - 1) == '"');
        }
    }

    /**
     * Optimized method for converting a String into an Integer
     * <p>
     * From http://stackoverflow.com/questions/1030479/most-efficient-way-of-converting-string-to-integer-in-java
     *
     * @param str the String holding an Integer value
     * @return the int value of str
     * @throws NumberFormatException if str cannot be parsed to an int
     */
    public static int intValueOf(String str) {
        int idx = 0;
        int end;
        boolean sign = false;
        char ch;

        if ((str == null) || ((end = str.length()) == 0) || ((((ch = str.charAt(0)) < '0') || (ch > '9')) && (!(sign = ch == '-') || (++idx == end) || ((ch = str.charAt(idx)) < '0') || (ch > '9')))) {
            throw new NumberFormatException(str);
        }

        int ival = 0;
        for (; ; ival *= 10) {
            ival += '0' - ch;
            if (++idx == end) {
                return sign ? ival : -ival;
            }
            if (((ch = str.charAt(idx)) < '0') || (ch > '9')) {
                throw new NumberFormatException(str);
            }
        }
    }

    /**
     * Optimized method for converting a String into an Integer
     * <p>
     * From http://stackoverflow.com/questions/1030479/most-efficient-way-of-converting-string-to-integer-in-java
     *
     * @param str the String holding an Integer value
     * @return the int value of str or Optional.empty() if not possible
     */
    public static Optional<Integer> intValueOfOptional(String str) {
        int idx = 0;
        int end;
        boolean sign = false;
        char ch;

        if ((str == null) || ((end = str.length()) == 0) || ((((ch = str.charAt(0)) < '0') || (ch > '9')) && (!(sign = ch == '-') || (++idx == end) || ((ch = str.charAt(idx)) < '0') || (ch > '9')))) {
            return Optional.empty();
        }

        int ival = 0;
        for (; ; ival *= 10) {
            ival += '0' - ch;
            if (++idx == end) {
                return Optional.of(sign ? ival : -ival);
            }
            if (((ch = str.charAt(idx)) < '0') || (ch > '9')) {
                return Optional.empty();
            }
        }
    }

    /**
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     * <p>
     * URL: http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    public static String stripNonValidXMLCharacters(String in) {
        if ((in == null) || in.isEmpty()) {
            return ""; // vacancy test.
        }
        StringBuilder out = new StringBuilder(); // Used to hold the output.
        char current; // Used to reference the current character.

        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) || (current == 0xA) || (current == 0xD) || ((current >= 0x20) && (current <= 0xD7FF))
                    || ((current >= 0xE000) && (current <= 0xFFFD))) {
                out.append(current);
            }
        }
        return out.toString();
    }

    /*
     * @param  buf       String to be tokenized
     * @param  delimstr  Delimiter string
     * @return list      {@link java.util.List} of <tt>String</tt>
     */
    public static List<String> tokenizeToList(String buf, String delimstr) {
        List<String> list = new ArrayList<>();
        String buffer = buf + '\n';

        StringTokenizer st = new StringTokenizer(buffer, delimstr);

        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }

        return list;
    }

    public static String limitStringLength(String s, int maxLength) {
        if (s == null) {
            return "";
        }

        if (s.length() <= maxLength) {
            return s;
        }

        return s.substring(0, maxLength - 3) + "...";
    }

    /**
     * Replace non-English characters like umlauts etc. with a sensible letter or letter combination that bibtex can
     * accept. The basis for replacement is the HashMap UnicodeToReadableCharMap.
     */
    public static String replaceSpecialCharacters(String s) {
        /* Some unicode characters can be encoded in multiple ways. This uses <a href="https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/text/Normalizer.Form.html#NFC">NFC</a>
         * to re-encode the characters so that these characters can be found.
         * Most people expect Unicode to work similar to NFC, i.e., if characters looks the same, it is likely that they are equivalent.
         * Hence, if someone debugs issues in the `UNICODE_CHAR_MAP`, they will expect NFC.
         * A more holistic approach should likely start with the <a href="http://unicode.org/reports/tr15/#Compatibility_Equivalence_Figure">compatibility equivalence</a>. */
        String result = Normalizer.normalize(s, Normalizer.Form.NFC);
        for (Map.Entry<String, String> chrAndReplace : UNICODE_CHAR_MAP.entrySet()) {
            result = result.replace(chrAndReplace.getKey(), chrAndReplace.getValue());
        }
        return result;
    }

    /**
     * Return a String with n spaces
     *
     * @param n Number of spaces
     * @return String with n spaces
     */
    public static String repeatSpaces(int n) {
        return repeat(Math.max(0, n), ' ');
    }

    /**
     * Return a String with n copies of the char c
     *
     * @param n Number of copies
     * @param c char to copy
     * @return String with n copies of c
     */
    public static String repeat(int n, char c) {
        StringBuilder resultSB = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            resultSB.append(c);
        }

        return resultSB.toString();
    }

    public static boolean isNullOrEmpty(String toTest) {
        return ((toTest == null) || toTest.isEmpty());
    }

    public static boolean isBlank(String string) {
        return !isNotBlank(string);
    }

    public static boolean isBlank(Optional<String> string) {
        return !isNotBlank(string);
    }

    /**
     * Checks if a CharSequence is not empty (""), not null and not whitespace only.
     */
    public static boolean isNotBlank(String string) {
        // No Guava equivalent existing
        return StringUtils.isNotBlank(string);
    }

    public static boolean isNotBlank(Optional<String> string) {
        return string.isPresent() && isNotBlank(string.get());
    }

    /**
     * Return string enclosed in HTML bold tags
     */
    public static String boldHTML(String input) {
        return "<b>" + input + "</b>";
    }

    /**
     * Return string enclosed in HTML bold tags  if not null, otherwise return alternative text in HTML bold tags
     */
    public static String boldHTML(String input, String alternative) {
        if (input == null) {
            return "<b>" + alternative + "</b>";
        }
        return "<b>" + input + "</b>";
    }

    /**
     * Unquote special characters.
     *
     * @param toUnquote The String which may contain quoted special characters.
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
            } else if (c == quoteChar) {
                // quote char
                quoted = true;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    @ApacheCommonsLang3Allowed("No Guava equivalent existing - see https://stackoverflow.com/q/3322152/873282 for a list of other implementations")
    public static String stripAccents(String searchQuery) {
        return StringUtils.stripAccents(searchQuery);
    }

    /**
     * Make first character of String uppercase, and the rest lowercase.
     */
    public static String capitalizeFirst(String toCapitalize) {
        if (toCapitalize.length() > 1) {
            return toCapitalize.substring(0, 1).toUpperCase(Locale.ROOT)
                    + toCapitalize.substring(1).toLowerCase(Locale.ROOT);
        } else {
            return toCapitalize.toUpperCase(Locale.ROOT);
        }

    }

    /**
     * Returns a list of words contained in the given text.
     * Whitespace, comma and semicolon are considered as separator between words.
     *
     * @param text the input
     * @return a list of words
     */
    public static List<String> getStringAsWords(String text) {
        return Arrays.asList(text.split("[\\s,;]+"));
    }

    /**
     * Returns a list of sentences contained in the given text.
     */
    public static List<String> getStringAsSentences(String text) {
        // A sentence ends with a .?!;, but not in the case of "Mr.", "Ms.", "Mrs.", "Dr.", "st.", "jr.", "co.", "inc.", and "ltd."
        Pattern splitTextPattern = Pattern.compile("(?<=[\\.!;\\?])(?<![Mm](([Rr]|[Rr][Ss])|[Ss])\\.|[Dd][Rr]\\.|[Ss][Tt]\\.|[Jj][Rr]\\.|[Cc][Oo]\\.|[Ii][Nn][Cc]\\.|[Ll][Tt][Dd]\\.)\\s+");
        return Arrays.asList(splitTextPattern.split(text));
    }

    @ApacheCommonsLang3Allowed("No direct Guava equivalent existing - see https://stackoverflow.com/q/16560635/873282")
    public static boolean containsIgnoreCase(String text, String searchString) {
        return StringUtils.containsIgnoreCase(text, searchString);
    }

    public static String substringBetween(String str, String open, String close) {
        return StringUtils.substringBetween(str, open, close);
    }

    public static String ignoreCurlyBracket(String title) {
        return isNotBlank(title) ? title.replace("{", "").replace("}", "") : title;
    }

    /**
     * Encloses the given string with " if there is a space contained
     *
     * @return Returns a string
     */
    public static String quoteStringIfSpaceIsContained(String string) {
        if (string.contains(" ")) {
            return "\"" + string + "\"";
        } else {
            return string;
        }
    }
}
