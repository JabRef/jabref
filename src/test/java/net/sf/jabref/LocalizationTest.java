package net.sf.jabref;

import com.google.common.base.Charsets;
import net.sf.jabref.logic.l10n.Localization;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class LocalizationTest {

    @Test
    public void findComplicatedKeys() {
        Localization.lang("Copy_\\=:cite{BibTeX_key}");
    }

    @Test
    public void findMissingLocalizationKeys() throws IOException {
        String missingKeys = LocalizationParser.find(LocalizationParser.Type.LANG).stream().map(Object::toString).collect(Collectors.joining("\n"));
        System.out.println(missingKeys);
        assertEquals("source code contains language keys for the messages which are not in the corresponding properties file", "", missingKeys);
    }

    @Test
    public void findMissingMenuLocalizationKeys() throws IOException {
        String missingKeys = LocalizationParser.find(LocalizationParser.Type.MENU).stream().map(Object::toString).collect(Collectors.joining("\n"));
        System.out.println(missingKeys);
        assertEquals("source code contains language keys for the menu which are not in the corresponding properties file",
                "", missingKeys);
    }

    @Test
    public void testParsingCode() {
        String code = ""
                + "Localization.lang(\"one per line\")"
                + "Localization.lang(\"two per line\") Localization.lang(\"two per line\")"
                + "Localization.lang(\"multi \" + \n"
                + "\"line\")"
                + "Localization.lang(\"one per line with var\", var)"
                + "Localization.lang(\"Search %0\", \"Springer\")"
                + "Localization.lang(\"Reset preferences (key1,key2,... or 'all')\")";

        List<String> expectedLanguageKeys = Arrays.asList("one_per_line", "two_per_line", "two_per_line", "multi_line",
                "one_per_line_with_var", "Search_%0", "Reset_preferences_(key1,key2,..._or_'all')");

        assertEquals(expectedLanguageKeys, LocalizationParser.getLanguageKeysInString(code, LocalizationParser.Type.LANG));
    }

    private static class LocalizationEntry {

        private final Path path;
        private final String key;
        private final int lineNumber;
        private final LocalizationParser.Type type;

        private LocalizationEntry(Path path, String key, int lineNumber, LocalizationParser.Type type) {
            this.path = path;
            this.key = key;
            this.lineNumber = lineNumber;
            this.type = type;
        }

        public Path getPath() {
            return path;
        }

        public String getKey() {
            return key;
        }

        public LocalizationParser.Type getType() {
            return type;
        }

        public String toString() {
            return String.format("%s:%d %s (%s)", path, lineNumber, key, type);
        }
    }

    private static class LocalizationParser {

        private static enum Type {
            LANG, MENU
        }

        private static List<LocalizationEntry> find(Type type) throws IOException {
            List<LocalizationEntry> entries = Files.walk(Paths.get("src/main"))
                    .filter(LocalizationParser::isJavaFile)
                    .flatMap(p -> getLanguageKeysInJavaFile(p, type).stream())
                    .collect(Collectors.toList());

            List<String> keysInJavaFiles = entries.stream()
                    .map(LocalizationEntry::getKey)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            Properties properties = new Properties();
            if (type == Type.LANG) {
                properties.load(LocalizationTest.class.getResourceAsStream("/l10n/JabRef_en.properties"));
            } else {
                properties.load(LocalizationTest.class.getResourceAsStream("/l10n/Menu_en.properties"));
            }

            List<String> englishKeys = properties.keySet().stream()
                    .sorted()
                    .map(Object::toString)
                    .map(String::trim)
                    .collect(Collectors.toList());
            List<String> missingKeys = new LinkedList<>(keysInJavaFiles);
            missingKeys.removeAll(englishKeys);

            return entries.stream().filter(e -> missingKeys.contains(e.getKey())).collect(Collectors.toList());
        }

        private static boolean isJavaFile(Path path) {
            return path.toString().endsWith(".java");
        }

        private static final String INFINITE_WHITESPACE = "\\s*";
        private static final String DOT = "\\.";
        private static final Pattern LOCALIZATION_START_PATTERN = Pattern.compile("Localization" + INFINITE_WHITESPACE + DOT + INFINITE_WHITESPACE + "lang" + INFINITE_WHITESPACE + "\\(");

        private static final Pattern LOCALIZATION_MENU_START_PATTERN = Pattern.compile("Localization" + INFINITE_WHITESPACE + DOT + INFINITE_WHITESPACE + "menuTitle" + INFINITE_WHITESPACE + "\\(");
        private static final Pattern ESCAPED_QUOTATION_SYMBOL = Pattern.compile("\\\\\"");

        private static final Pattern QUOTATION_SYMBOL = Pattern.compile("QUOTATIONPLACEHOLDER");

        private static List<LocalizationEntry> getLanguageKeysInJavaFile(Path path, Type type) {
            List<LocalizationEntry> result = new LinkedList<>();

            try {
                String content = String.join("\n", Files.readAllLines(path, Charsets.UTF_8));

                List<String> keys = getLanguageKeysInString(content, type);

                for (String key : keys) {
                    result.add(new LocalizationEntry(path, key, 1, type));
                }

            } catch (IOException ignore) {
                ignore.printStackTrace();
            }

            return result;
        }

        private static List<String> getLanguageKeysInString(String content, Type type) {
            List<String> result = new LinkedList<>();

            Matcher matcher;
            if (type == Type.LANG) {
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
                    if (c == '"' && quotations > 0) {
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
                String languagePropertyKey = languageKey.replaceAll(" ", "_").replaceAll("=", "\\=").replaceAll(":", "\\:");

                if (languagePropertyKey.endsWith("_")) {
                    throw new RuntimeException("Key " + languageKey + " ends with a space");
                }

                if (!languagePropertyKey.trim().isEmpty()) {
                    result.add(languagePropertyKey);
                }

            }

            return result;
        }

    }

}
