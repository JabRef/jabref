package org.jabref.logic.l10n;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            if (!languagePropertyKey.trim().isEmpty()) {
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

    public static List<String> getLocalizationParameter(String content, LocalizationBundleForTest type) {
        List<String> result = new ArrayList<>();

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
}
