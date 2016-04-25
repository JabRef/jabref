package net.sf.jabref.logic.l10n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LocalizationParser {

    public static List<LocalizationEntry> find(LocalizationBundle type) throws IOException {
        List<LocalizationEntry> entries = findLocalizationEntriesInJavaFiles(type);

        List<String> keysInJavaFiles = entries.stream()
                .map(LocalizationEntry::getKey)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> englishKeys;
        if (type == LocalizationBundle.LANG) {
            englishKeys = getKeysInPropertiesFile("/l10n/JabRef_en.properties");
        } else {
            englishKeys = getKeysInPropertiesFile("/l10n/Menu_en.properties");
        }
        List<String> missingKeys = new LinkedList<>(keysInJavaFiles);
        missingKeys.removeAll(englishKeys);

        return entries.stream().filter(e -> missingKeys.contains(e.getKey())).collect(Collectors.toList());
    }

    public static List<String> findObsolete(LocalizationBundle type) throws IOException {
        List<LocalizationEntry> entries = findLocalizationEntriesInJavaFiles(type);

        List<String> keysInJavaFiles = entries.stream().map(LocalizationEntry::getKey).distinct().sorted()
                .collect(Collectors.toList());

        List<String> englishKeys;
        if (type == LocalizationBundle.LANG) {
            englishKeys = getKeysInPropertiesFile("/l10n/JabRef_en.properties");
        } else {
            englishKeys = getKeysInPropertiesFile("/l10n/Menu_en.properties");
        }
        englishKeys.removeAll(keysInJavaFiles);

        return englishKeys;
    }

    private static List<LocalizationEntry> findLocalizationEntriesInJavaFiles(LocalizationBundle type)
            throws IOException {
        return Files.walk(Paths.get("src/main"))
                .filter(LocalizationParser::isJavaFile)
                .flatMap(p -> getLanguageKeysInJavaFile(p, type).stream())
                .collect(Collectors.toList());
    }

    public static List<String> getKeysInPropertiesFile(String path) {
        Properties properties = getProperties(path);

        return properties.keySet().stream()
                .sorted()
                .map(Object::toString)
                .map(String::trim)
                .map(e -> new LocalizationKey(e).getPropertiesKey())
                .collect(Collectors.toList());
    }

    public static Properties getProperties(String path) {
        Properties properties = new Properties();
        try (InputStream is = LocalizationConsistencyTest.class.getResourceAsStream(path);
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    private static boolean isJavaFile(Path path) {
        return path.toString().endsWith(".java");
    }

    public static List<LocalizationEntry> getLanguageKeysInJavaFile(Path path, LocalizationBundle type) {
        List<LocalizationEntry> result = new LinkedList<>();

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String content = String.join("\n", lines);

            List<String> keys = JavaLocalizationEntryParser.getLanguageKeysInString(content, type);

            for (String key : keys) {
                result.add(new LocalizationEntry(path, key, type));
            }

        } catch (IOException ignore) {
            ignore.printStackTrace();
        }

        return result;
    }

    public static class JavaLocalizationEntryParser {

        private static final String INFINITE_WHITESPACE = "\\s*";
        private static final String DOT = "\\.";
        private static final Pattern LOCALIZATION_START_PATTERN = Pattern.compile("Localization" + INFINITE_WHITESPACE + DOT + INFINITE_WHITESPACE + "lang" + INFINITE_WHITESPACE + "\\(");

        private static final Pattern LOCALIZATION_MENU_START_PATTERN = Pattern.compile("Localization" + INFINITE_WHITESPACE + DOT + INFINITE_WHITESPACE + "menuTitle" + INFINITE_WHITESPACE + "\\(");
        private static final Pattern ESCAPED_QUOTATION_SYMBOL = Pattern.compile("\\\\\"");

        private static final Pattern QUOTATION_SYMBOL = Pattern.compile("QUOTATIONPLACEHOLDER");

        public static List<String> getLanguageKeysInString(String content, LocalizationBundle type) {
            List<String> result = new LinkedList<>();

            Matcher matcher;
            if (type == LocalizationBundle.LANG) {
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
                    buffer.append(c);
                    index++;
                }

                String parsedContentsOfLangMethod = ESCAPED_QUOTATION_SYMBOL.matcher(buffer.toString()).replaceAll("QUOTATIONPLACEHOLDER");

                // only retain what is within quotation
                StringBuilder b = new StringBuilder();
                int quotations = 0;
                for (char c : parsedContentsOfLangMethod.toCharArray()) {
                    if ((c == '"') && (quotations > 0)) {
                        quotations--;
                    } else if (c == '"') {
                        quotations++;
                    } else {
                        if (quotations != 0) {
                            b.append(c);
                        } else {
                            if (c == ',') {
                                break;
                            }
                        }
                    }
                }

                String languageKey = QUOTATION_SYMBOL.matcher(b.toString()).replaceAll("\\\"");

                // escape chars which are not allowed in property file keys
                String languagePropertyKey = new LocalizationKey(languageKey).getPropertiesKey();

                if (languagePropertyKey.endsWith("_")) {
                    throw new RuntimeException(languageKey + " ends with a space. As this is a localization key, this is illegal!");
                }

                if (languagePropertyKey.contains("\\n")) {
                    throw new RuntimeException(languageKey + " contains a new line character. As this is a localization key, this is illegal!");
                }

                if (!languagePropertyKey.trim().isEmpty()) {
                    result.add(languagePropertyKey);
                }

            }

            return result;
        }

    }

}
