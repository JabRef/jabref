package org.jabref.logic.l10n;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("Localization.lang")
class JavaLocalizationEntryParser {

    private static final String INFINITE_WHITESPACE = "\\s*";
    private static final String DOT = "\\.";
    private static final Pattern LOCALIZATION_START_PATTERN = Pattern.compile("Localization" + INFINITE_WHITESPACE + DOT + INFINITE_WHITESPACE + "lang" + INFINITE_WHITESPACE + "\\(");

    private static final Pattern LOCALIZATION_MENU_START_PATTERN = Pattern.compile("Localization" + INFINITE_WHITESPACE + DOT + INFINITE_WHITESPACE + "menuTitle" + INFINITE_WHITESPACE + "\\(");
    private static final Pattern ESCAPED_QUOTATION_SYMBOL = Pattern.compile("\\\\\"");

    private static final String QUOTATION_PLACEHOLDER = "QUOTATIONPLACEHOLDER";
    private static final Pattern QUOTATION_SYMBOL_PATTERN = Pattern.compile(QUOTATION_PLACEHOLDER);

    public static List<String> getLanguageKeysInString(String content, LocalizationBundleForTest type) {
        List<String> parameters = getLocalizationParameter(content, type);

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

    public static List<String> getLocalizationParameter(String rawContent, LocalizationBundleForTest type) {
        List<String> result = new ArrayList<>();

        // Comments may contain `Localization.lang(...)` snippets, which are no real usages.
        String content = blankOutComments(rawContent);

        Matcher matcher;
        if (type == LocalizationBundleForTest.LANG) {
            matcher = LOCALIZATION_START_PATTERN.matcher(content);
        } else {
            matcher = LOCALIZATION_MENU_START_PATTERN.matcher(content);
        }
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
    /// Newlines are kept, so that the result has the same length as the input and the same line structure.
    /// String literals, text blocks, and character literals are left untouched -
    /// a `//` inside a string (e.g., a URL) does not start a comment.
    static String blankOutComments(String source) {
        char[] chars = source.toCharArray();
        int length = chars.length;
        int i = 0;
        while (i < length) {
            char current = chars[i];
            char next = (i + 1) < length ? chars[i + 1] : '\0';
            if ((current == '/') && (next == '/')) {
                while ((i < length) && (chars[i] != '\n')) {
                    chars[i] = ' ';
                    i++;
                }
            } else if ((current == '/') && (next == '*')) {
                chars[i] = ' ';
                chars[i + 1] = ' ';
                i += 2;
                while (i < length) {
                    if ((chars[i] == '*') && ((i + 1) < length) && (chars[i + 1] == '/')) {
                        chars[i] = ' ';
                        chars[i + 1] = ' ';
                        i += 2;
                        break;
                    }
                    if (chars[i] != '\n') {
                        chars[i] = ' ';
                    }
                    i++;
                }
            } else if ((current == '"') && (next == '"') && ((i + 2) < length) && (chars[i + 2] == '"')) {
                // text block
                i += 3;
                while (i < length) {
                    if (chars[i] == '\\') {
                        i += 2;
                    } else if ((chars[i] == '"') && ((i + 2) < length) && (chars[i + 1] == '"') && (chars[i + 2] == '"')) {
                        i += 3;
                        break;
                    } else {
                        i++;
                    }
                }
            } else if ((current == '"') || (current == '\'')) {
                i++;
                while (i < length) {
                    if (chars[i] == '\\') {
                        i += 2;
                    } else if (chars[i] == current) {
                        i++;
                        break;
                    } else {
                        i++;
                    }
                }
            } else {
                i++;
            }
        }
        return new String(chars);
    }
}
