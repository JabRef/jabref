package org.jabref.logic.l10n;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("Localization.lang")
class JavaLocalizationEntryParser {

    private enum CommentState {
        NORMAL,
        LINE_COMMENT,
        BLOCK_COMMENT,
        STRING,
        CHARACTER,
        TEXT_BLOCK
    }

    private static final String INFINITE_WHITESPACE = "\\s*";
    private static final String DOT = "\\.";
    private static final Pattern LOCALIZATION_START_PATTERN = Pattern.compile("Localization" + INFINITE_WHITESPACE + DOT + INFINITE_WHITESPACE + "lang" + INFINITE_WHITESPACE + "\\(");

    private static final Pattern ESCAPED_QUOTATION_SYMBOL = Pattern.compile("\\\\\"");

    private static final String QUOTATION_PLACEHOLDER = "QUOTATIONPLACEHOLDER";
    private static final Pattern QUOTATION_SYMBOL_PATTERN = Pattern.compile(QUOTATION_PLACEHOLDER);

    public static List<String> getLanguageKeysInString(String content) {
        List<String> parameters = getLocalizationParameter(content);

        List<String> result = new ArrayList<>();

        for (String param : parameters) {
            String languageKey = getContentWithinQuotes(param);
            if (languageKey.contains("\\\n") || languageKey.contains("\\\\n")) {
                // see also https://stackoverflow.com/a/10285687/873282
                // '\n' (newline character) in the language key is stored as text "\n" in the .properties file. This is OK.
                throw new RuntimeException("\"" + languageKey + "\" contains an escaped new line character. The newline character has to be written with a single backslash, not with a double one: \\n is correct, \\\\n is wrong.");
            }

            // Java escape chars which are not used in property file keys
            // The call to `getPropertiesKey` escapes them
            String languagePropertyKey = LocalizationKey.fromEscapedJavaString(languageKey).getKey();

            if (languagePropertyKey.endsWith(" ")) {
                throw new RuntimeException("\"" + languageKey + "\" ends with a space. As this is a localization key, this is illegal!");
            }

            if (!languagePropertyKey.isBlank()) {
                result.add(languagePropertyKey);
            }
        }

        return result;
    }

    private static String getContentWithinQuotes(String param) {
        // protect \" in string
        String contentWithProtectedEscapedQuote = ESCAPED_QUOTATION_SYMBOL.matcher(param).replaceAll(QUOTATION_PLACEHOLDER);

        // extract text between "..."
        StringBuilder stringBuilder = new StringBuilder();
        int quotations = 0;
        for (char currentCharacter : contentWithProtectedEscapedQuote.toCharArray()) {
            if ((currentCharacter == '"') && (quotations > 0)) {
                quotations--;
            } else if (currentCharacter == '"') {
                quotations++;
            } else if (quotations != 0) {
                stringBuilder.append(currentCharacter);
            } else if (currentCharacter == ',') {
                break;
            }
        }

        // re-introduce \" (escaped quotes) into string
        String languageKey = QUOTATION_SYMBOL_PATTERN.matcher(stringBuilder.toString()).replaceAll("\\\"");

        return languageKey;
    }

    public static List<String> getLocalizationParameter(String rawContent) {
        List<String> result = new ArrayList<>();

        // Comments may contain `Localization.lang(...)` snippets, which are no real usages.
        String content = blankOutComments(rawContent);

        Matcher matcher = LOCALIZATION_START_PATTERN.matcher(content);
        while (matcher.find()) {
            // find contents between the brackets, covering multi-line strings as well
            int index = matcher.end();
            int brackets = 1;
            StringBuilder buffer = new StringBuilder();
            while (brackets != 0) {
                char c = content.charAt(index);
                if (c == '(') {
                    brackets++;
                } else if (c == ')') {
                    brackets--;
                }
                // skip closing brackets
                if (brackets != 0) {
                    buffer.append(c);
                }
                index++;
            }
            // trim newlines and whitespace
            result.add(buffer.toString().trim());
        }

        return result;
    }

    /// Replaces the content of all Java comments (`//`, `///`, `/* */`, javadoc) by spaces.
    /// Newlines are kept, so that the result has the same length as the input and the same
    /// line structure. String literals, text blocks, and character literals are left untouched,
    /// so a `//` inside a string (e.g. a URL) does not start a comment.
    static String blankOutComments(String source) {
        char[] chars = source.toCharArray();

        CommentState state = CommentState.NORMAL;
        char quote = '\0';

        for (int i = 0; i < chars.length; i++) {
            switch (state) {
                case NORMAL -> {
                    if (startsWith(chars, i, '/', '/')) {
                        chars[i] = ' ';
                        chars[i + 1] = ' ';
                        i++;
                        state = CommentState.LINE_COMMENT;
                    } else if (startsWith(chars, i, '/', '*')) {
                        chars[i] = ' ';
                        chars[i + 1] = ' ';
                        i++;
                        state = CommentState.BLOCK_COMMENT;
                    } else if (startsTextBlock(chars, i)) {
                        i += 2;
                        state = CommentState.TEXT_BLOCK;
                    } else if (chars[i] == '"') {
                        quote = '"';
                        state = CommentState.STRING;
                    } else if (chars[i] == '\'') {
                        quote = '\'';
                        state = CommentState.CHARACTER;
                    }
                }

                case LINE_COMMENT -> {
                    if (chars[i] == '\n') {
                        state = CommentState.NORMAL;
                    } else {
                        chars[i] = ' ';
                    }
                }

                case BLOCK_COMMENT -> {
                    if (startsWith(chars, i, '*', '/')) {
                        chars[i] = ' ';
                        chars[i + 1] = ' ';
                        i++;
                        state = CommentState.NORMAL;
                    } else if (chars[i] != '\n') {
                        chars[i] = ' ';
                    }
                }

                case STRING,
                     CHARACTER -> {
                    if (chars[i] == '\\') {
                        i++; // skip escaped character
                    } else if (chars[i] == quote) {
                        state = CommentState.NORMAL;
                    }
                }

                case TEXT_BLOCK -> {
                    if (chars[i] == '\\') {
                        i++; // skip escaped character
                    } else if (startsTextBlock(chars, i)) {
                        i += 2;
                        state = CommentState.NORMAL;
                    }
                }
            }
        }

        return new String(chars);
    }

    private static boolean startsWith(char[] chars, int index, char first, char second) {
        return index + 1 < chars.length
                && chars[index] == first
                && chars[index + 1] == second;
    }

    private static boolean startsTextBlock(char[] chars, int index) {
        return index + 2 < chars.length
                && chars[index] == '"'
                && chars[index + 1] == '"'
                && chars[index + 2] == '"';
    }
}
