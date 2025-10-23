package org.jabref.logic.util.strings;

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

import org.jabref.architecture.AllowedToUseApacheCommonsLang3;
import org.jabref.logic.os.OS;

import com.google.common.base.CharMatcher;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("checkstyle:NoMultipleClosingBracesAtEndOfLine")
@AllowedToUseApacheCommonsLang3("There is no equivalent in Google's Guava")
public class StringUtil {

    // Non-letters which are used to denote accents in LaTeX-commands, e.g., in {\"{a}}
    public static final String SPECIAL_COMMAND_CHARS = "\"`^~'=.|_#$&";
    // contains all possible line breaks, not omitting any break such as "\\n"
    private static final Pattern LINE_BREAKS = Pattern.compile("\\r\\n|\\r|\\n");
    private static final Pattern BRACED_TITLE_CAPITAL_PATTERN = Pattern.compile("\\{[A-Z]+\\}");

    /**
     * Pattern for normalizing whitespace and punctuation using named capture groups
     */
    private static final Pattern NORMALIZE_PATTERN = Pattern.compile(
            "(?<whitespace>\\s+)|" +                   // multiple whitespace
                    "(?<hyphen>\\s*-+\\s*)|" +         // hyphens with surrounding spaces
                    "(?<comma>\\s*,\\s*)|" +           // commas with surrounding spaces
                    "(?<semicolon>\\s*;\\s*)|" +       // semicolons with surrounding spaces
                    "(?<colon>\\s*:\\s*)"              // colons with surrounding spaces
    );

    private static final UnicodeToReadableCharMap UNICODE_CHAR_MAP = new UnicodeToReadableCharMap();
    private static final String WRAPPED_LINE_PREFIX = ""; // If a line break is added, this prefix will be inserted at the beginning of the next line
    private static final String STRING_TABLE_DELIMITER = " : ";

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

            isSpecial = c == quoteChar;
            // If non-null specials performs logic-or with specials.indexOf(c) >= 0
            isSpecial |= (specials != null) && (specials.indexOf(c) >= 0);

            if (isSpecial) {
                result.append(quoteChar);
            }
            result.append(c);
        }
        return result.toString();
    }

    /**
     * Creates a substring from a text
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
     * @param to Excluding strings[to]
     */
    public static String join(String[] strings, String separator, int from, int to) {
        if ((strings == null) || (strings.length == 0) || (from >= to)) {
            return "";
        }

        int updatedFrom = Math.max(0, from);
        int updatedTo = Math.min(strings.length, to);

        if (updatedFrom >= updatedTo) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = updatedFrom; i < (updatedTo - 1); i++) {
            stringBuilder.append(strings[i]).append(separator);
        }
        return stringBuilder.append(strings[updatedTo - 1]).toString();
    }

    /**
     * Removes optional square brackets from the string s
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
}