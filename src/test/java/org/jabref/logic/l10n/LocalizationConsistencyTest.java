package org.jabref.logic.l10n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Need to run on JavaFX thread since we are parsing FXML files
@ExtendWith(ApplicationExtension.class)
class LocalizationConsistencyTest {

    @Test
    void allFilesMustBeInLanguages() throws IOException {
        String bundle = "JabRef";
        // e.g., "<bundle>_en.properties", where <bundle> is [JabRef, Menu]
        Pattern propertiesFile = Pattern.compile(String.format("%s_.{2,}.properties", bundle));
        Set<String> localizationFiles = new HashSet<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Path.of("src/main/resources/l10n"))) {
            for (Path fullPath : directoryStream) {
                String fileName = fullPath.getFileName().toString();
                if (propertiesFile.matcher(fileName).matches()) {
                    localizationFiles.add(fileName.substring(bundle.length() + 1, fileName.length() - ".properties".length()));
                }
            }
        }

        Set<String> knownLanguages = Stream.of(Language.values())
                                           .map(Language::getId)
                                           .collect(Collectors.toSet());
        assertEquals(knownLanguages, localizationFiles, "There are some localization files that are not present in org.jabref.logic.l10n.Language or vice versa!");
    }

    @Test
    void ensureNoDuplicates() {
        String bundle = "JabRef";
        for (Language lang : Language.values()) {
            String propertyFilePath = String.format("/l10n/%s_%s.properties", bundle, lang.getId());

            // read in
            DuplicationDetectionProperties properties = new DuplicationDetectionProperties();
            try (InputStream is = LocalizationConsistencyTest.class.getResourceAsStream(propertyFilePath);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<String> duplicates = properties.getDuplicates();

            assertEquals(Collections.emptyList(), duplicates, "Duplicate keys inside bundle " + bundle + "_" + lang.getId());
        }
    }

    @Test
    void keyValueShouldBeEqualForEnglishPropertiesMessages() {
        Properties englishKeys = LocalizationParser.getProperties(String.format("/l10n/%s_%s.properties", "JabRef", "en"));
        for (Map.Entry<Object, Object> entry : englishKeys.entrySet()) {
            String expectedKeyEqualsKey = String.format("%s=%s", entry.getKey(), entry.getKey().toString().replace("\n", "\\n"));
            String actualKeyEqualsValue = String.format("%s=%s", entry.getKey(), entry.getValue().toString().replace("\n", "\\n"));
            assertEquals(expectedKeyEqualsKey, actualKeyEqualsValue);
        }
    }

    @Test
    void languageKeysShouldNotContainUnderscoresForSpaces() throws IOException {
        final List<LocalizationEntry> quotedEntries = LocalizationParser
                .findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest.LANG)
                .stream()
                .filter(key -> key.getKey().contains("\\_"))
                .collect(Collectors.toList());
        assertEquals(Collections.emptyList(), quotedEntries,
                "Language keys must not use underscores for spaces! Use \"This is a message\" instead of \"This_is_a_message\".\n" +
                        "Please correct the following entries:\n" +
                        quotedEntries
                                .stream()
                                .map(key -> String.format("\n%s (%s)\n", key.getKey(), key.getPath()))
                                .collect(Collectors.toList()));
    }

    @Test
    void languageKeysShouldNotContainHtmlBrAndHtmlP() throws IOException {
        final List<LocalizationEntry> entriesWithHtml = LocalizationParser
                .findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest.LANG)
                .stream()
                .filter(key -> key.getKey().contains("<br>") || key.getKey().contains("<p>"))
                .collect(Collectors.toList());
        assertEquals(Collections.emptyList(), entriesWithHtml,
                "Language keys must not contain HTML <br> or <p>. Use \\n for a line break.\n" +
                        "Please correct the following entries:\n" +
                        entriesWithHtml
                                .stream()
                                .map(key -> String.format("\n%s (%s)\n", key.getKey(), key.getPath()))
                                .collect(Collectors.toList()));
    }

    @Test
    void findMissingLocalizationKeys() throws IOException {
        List<LocalizationEntry> missingKeys = LocalizationParser.findMissingKeys(LocalizationBundleForTest.LANG)
                                                                .stream()
                                                                .collect(Collectors.toList());
        assertEquals(Collections.emptyList(), missingKeys,
                missingKeys.stream()
                           .map(key -> LocalizationKey.fromKey(key.getKey()))
                           .map(key -> String.format("%s=%s",
                                   key.getEscapedPropertiesKey(),
                                   key.getValueForEnglishPropertiesFile()))
                           .collect(Collectors.joining("\n",
                                   "\n\nDETECTED LANGUAGE KEYS WHICH ARE NOT IN THE ENGLISH LANGUAGE FILE\n" +
                                           "PASTE THESE INTO THE ENGLISH LANGUAGE FILE\n\n",
                                   "\n\n")));
    }

    @Test
    void findObsoleteLocalizationKeys() throws IOException {
        Set<String> obsoleteKeys = LocalizationParser.findObsolete(LocalizationBundleForTest.LANG);
        assertEquals(Collections.emptySet(), obsoleteKeys,
                obsoleteKeys.stream().collect(Collectors.joining("\n",
                        "Obsolete keys found in language properties file: \n\n",
                        "\n\n1. CHECK IF THE KEY IS REALLY NOT USED ANYMORE\n" +
                                "2. REMOVE THESE FROM THE ENGLISH LANGUAGE FILE\n"))
        );
    }

    @Test
    void localizationParameterMustIncludeAString() throws IOException {
        // Must start or end with "
        // Localization.lang("test"), Localization.lang("test" + var), Localization.lang(var + "test")
        // TODO: Localization.lang(var1 + "test" + var2) not covered
        // Localization.lang("Problem downloading from %1", address)
        Set<LocalizationEntry> keys = LocalizationParser.findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest.LANG);
        for (LocalizationEntry e : keys) {
            assertTrue(e.getKey().startsWith("\"") || e.getKey().endsWith("\""), "Illegal localization parameter found. Must include a String with potential concatenation or replacement parameters. Illegal parameter: Localization.lang(" + e.getKey());
        }

        keys = LocalizationParser.findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest.MENU);
        for (LocalizationEntry e : keys) {
            assertTrue(e.getKey().startsWith("\"") || e.getKey().endsWith("\""), "Illegal localization parameter found. Must include a String with potential concatenation or replacement parameters. Illegal parameter: Localization.lang(" + e.getKey());
        }
    }

    private static Language[] installedLanguages() {
        return Language.values();
    }

    @ParameterizedTest
    @MethodSource("installedLanguages")
    void resourceBundleExists(Language language) {
        Path messagesPropertyFile = Path.of("src/main/resources").resolve(Localization.RESOURCE_PREFIX + "_" + language.getId() + ".properties");
        assertTrue(Files.exists(messagesPropertyFile));
    }

    @ParameterizedTest
    @MethodSource("installedLanguages")
    void languageCanBeLoaded(Language language) {
        Locale oldLocale = Locale.getDefault();
        try {
            Locale locale = Language.convertToSupportedLocale(language).get();
            Locale.setDefault(locale);
            ResourceBundle messages = ResourceBundle.getBundle(Localization.RESOURCE_PREFIX, locale);
            assertNotNull(messages);
        } finally {
            Locale.setDefault(oldLocale);
        }
    }

    private static class DuplicationDetectionProperties extends Properties {

        private static final long serialVersionUID = 1L;

        private final List<String> duplicates = new ArrayList<>();

        DuplicationDetectionProperties() {
            super();
        }

        /**
         * Overriding the HashTable put() so we can check for duplicates
         */
        @Override
        public synchronized Object put(Object key, Object value) {
            // Have we seen this key before?
            if (containsKey(key)) {
                duplicates.add(String.valueOf(key));
            }

            return super.put(key, value);
        }

        List<String> getDuplicates() {
            return duplicates;
        }
    }
}
