package org.jabref.logic.l10n;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizationConsistencyTest {

    @Test
    void allFilesMustBeInLanguages() throws IOException {
        String bundle = "JabRef";
        // e.g., "<bundle>_en.properties", where <bundle> is [JabRef, Menu]
        Pattern propertiesFile = Pattern.compile(String.format("%s_.{2,}.properties", bundle));
        Set<String> localizationFiles = new HashSet<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get("src/main/resources/l10n"))) {
            for (Path fullPath : directoryStream) {
                String fileName = fullPath.getFileName().toString();
                if (propertiesFile.matcher(fileName).matches()) {
                    localizationFiles.add(fileName.substring(bundle.length() + 1, fileName.length() - ".properties".length()));
                }
            }
        }
        assertEquals(Collections.emptySet(), Sets.symmetricDifference(new HashSet<>(Languages.LANGUAGES.values()), localizationFiles), "There are some localization files that are not present in org.jabref.logic.l10n.Languages or vice versa!");
    }

    @Test
    void ensureNoDuplicates() {
        String bundle = "JabRef";
        for (String lang : Languages.LANGUAGES.values()) {
            String propertyFilePath = String.format("/l10n/%s_%s.properties", bundle, lang);

            // read in
            DuplicationDetectionProperties properties = new DuplicationDetectionProperties();
            try (InputStream is = LocalizationConsistencyTest.class.getResourceAsStream(propertyFilePath);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<String> duplicates = properties.getDuplicates();

            assertEquals(Collections.emptyList(), duplicates, "Duplicate keys inside bundle " + bundle + "_" + lang);
        }
    }

    @Test
    void keyValueShouldBeEqualForEnglishPropertiesMessages() {
        Properties englishKeys = LocalizationParser.getProperties(String.format("/l10n/%s_%s.properties", "JabRef", "en"));
        for (Map.Entry<Object, Object> entry : englishKeys.entrySet()) {
            String expectedKeyEqualsKey = String.format("%s=%s", entry.getKey(), entry.getKey());
            String actualKeyEqualsValue = String.format("%s=%s", entry.getKey(), entry.getValue());
            assertEquals(expectedKeyEqualsKey, actualKeyEqualsValue);
        }
    }

    @Test
    void languageKeysShouldNotBeQuotedInFiles() throws IOException {
        final List<LocalizationEntry> quotedEntries = LocalizationParser
                .findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest.LANG)
                .stream()
                .filter(key -> key.getKey().contains("_") && key.getKey().equals(new LocalizationKey(key.getKey()).getPropertiesKey()))
                .collect(Collectors.toList());

        assertEquals(Collections.EMPTY_LIST, quotedEntries,
                "Language keys must not be used quoted in code! Use \"This is a message\" instead of \"This_is_a_message\".\n" +
                "Please correct the following entries:\n" +
                quotedEntries
                        .stream()
                        .map(key -> String.format("\n%s (%s)\n", key.getKey(), key.getPath()))
                        .collect(Collectors.toList()));
    }

    @Test
    void findMissingLocalizationKeys() throws IOException {
        List<LocalizationEntry> missingKeys = LocalizationParser.find(LocalizationBundleForTest.LANG)
                .stream()
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        assertEquals(Collections.emptyList(), missingKeys,
                "DETECTED LANGUAGE KEYS WHICH ARE NOT IN THE ENGLISH LANGUAGE FILE\n" +
                "PASTE THESE INTO THE ENGLISH LANGUAGE FILE\n" +
                missingKeys.parallelStream()
                        .map(key -> String.format("\n%s=%s\n", key.getKey(), key.getKey().replaceAll("\\\\ ", " ")))
                        .collect(Collectors.joining("\n")));
    }

    @Test
    void findObsoleteLocalizationKeys() throws IOException {
        Set<String> obsoleteKeys = LocalizationParser.findObsolete(LocalizationBundleForTest.LANG);

        assertEquals(Collections.emptySet(), obsoleteKeys,
                "Obsolete keys found in language properties file: \n" +
                obsoleteKeys.stream().collect(Collectors.joining("\n")) +
                "\n" +
                "1. CHECK IF THE KEY IS REALLY NOT USED ANYMORE\n" +
                "2. REMOVE THESE FROM THE ENGLISH LANGUAGE FILE\n");
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
