package net.sf.jabref.logic.l10n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LocalizationConsistencyTest {

    @Test
    public void allFilesMustHaveSameKeys() {
        for (String bundle : Arrays.asList("JabRef", "Menu")) {
            List<String> englishKeys = LocalizationParser
                    .getKeysInPropertiesFile(String.format("/l10n/%s_%s.properties", bundle, "en"));

            List<String> nonEnglishLanguages = Languages.LANGUAGES.values().stream().filter(l -> !"en".equals(l))
                    .collect(Collectors.toList());
            for (String lang : nonEnglishLanguages) {
                List<String> nonEnglishKeys = LocalizationParser
                        .getKeysInPropertiesFile(String.format("/l10n/%s_%s.properties", bundle, lang));

                List<String> missing = new LinkedList<>(englishKeys);
                missing.removeAll(nonEnglishKeys);
                List<String> obsolete = new LinkedList<>(nonEnglishKeys);
                obsolete.removeAll(englishKeys);

                assertEquals("Missing keys of " + lang, Collections.emptyList(), missing);
                assertEquals("Obsolete keys of " + lang, Collections.emptyList(), obsolete);
            }
        }
    }


    private static class DuplicationDetectionProperties extends Properties {

        private static final long serialVersionUID = 1L;

        private final List<String> duplicates = new LinkedList<>();


        public DuplicationDetectionProperties() {
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

        public List<String> getDuplicates() {
            return duplicates;
        }
    }


    @Test
    public void ensureNoDuplicates() {
        for (String bundle : Arrays.asList("JabRef", "Menu")) {
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

                assertEquals("Duplicate keys inside bundle " + bundle + "_" + lang, Collections.emptyList(), duplicates);
            }
        }
    }

    @Test
    public void keyValueShouldBeEqualForEnglishPropertiesMenu() {
        Properties englishKeys = LocalizationParser
                .getProperties(String.format("/l10n/%s_%s.properties", "Menu", "en"));
        for (Map.Entry<Object, Object> entry : englishKeys.entrySet()) {
            String expectedKeyEqualsKey = String.format("%s=%s", entry.getKey(), entry.getKey());
            String actualKeyEqualsValue = String.format("%s=%s", entry.getKey(),
                    entry.getValue().toString().replace("&", ""));
            assertEquals(expectedKeyEqualsKey, actualKeyEqualsValue);
        }
    }

    @Test
    public void keyValueShouldBeEqualForEnglishPropertiesMessages() {
        Properties englishKeys = LocalizationParser
                .getProperties(String.format("/l10n/%s_%s.properties", "JabRef", "en"));
        for (Map.Entry<Object, Object> entry : englishKeys.entrySet()) {
            String expectedKeyEqualsKey = String.format("%s=%s", entry.getKey(), entry.getKey());
            String actualKeyEqualsValue = String.format("%s=%s", entry.getKey(), entry.getValue());
            assertEquals(expectedKeyEqualsKey, actualKeyEqualsValue);
        }
    }

    @Test
    public void findMissingLocalizationKeys() throws IOException {
        List<LocalizationEntry> missingKeys = LocalizationParser.find(LocalizationBundle.LANG).stream().sorted()
                .distinct().collect(Collectors.toList());

        assertEquals("DETECTED LANGUAGE KEYS WHICH ARE NOT IN THE ENGLISH LANGUAGE FILE\n" +
                "1. PASTE THESE INTO THE ENGLISH LANGUAGE FILE\n" +
                "2. EXECUTE: gradlew localizationUpdate\n" +
                missingKeys.parallelStream()
                        .map(key -> String.format("%s=%s", key.getKey(), key.getKey()))
                        .collect(Collectors.toList()),
                Collections.<LocalizationEntry>emptyList(), missingKeys);
    }

    @Test
    public void findMissingMenuLocalizationKeys() throws IOException {
        List<LocalizationEntry> missingKeys = LocalizationParser.find(LocalizationBundle.MENU).stream()
                .collect(Collectors.toList());

        assertEquals("DETECTED LANGUAGE KEYS WHICH ARE NOT IN THE ENGLISH MENU FILE\n" +
                "1. PASTE THESE INTO THE ENGLISH MENU FILE\n" +
                "2. EXECUTE: gradlew localizationUpdate\n" +
                missingKeys.parallelStream()
                        .map(key -> String.format("%s=%s", key.getKey(), key.getKey()))
                        .collect(Collectors.toList()),
                Collections.<LocalizationEntry>emptyList(), missingKeys);
    }

    @Test
    public void findObsoleteLocalizationKeys() throws IOException {
        List<String> obsoleteKeys = LocalizationParser.findObsolete(LocalizationBundle.LANG);

        assertEquals("Obsolete keys found in language properties file: " + obsoleteKeys + "\n" +
                "1. CHECK IF THE KEY IS REALLY NOT USED ANYMORE\n" +
                "2. REMOVE THESE FROM THE ENGLISH LANGUAGE FILE\n" +
                "3. EXECUTE: gradlew localizationUpdate\n",
                Collections.<String>emptyList(), obsoleteKeys);
    }

    @Test
    public void findObsoleteMenuLocalizationKeys() throws IOException {
        List<String> obsoleteKeys = LocalizationParser.findObsolete(LocalizationBundle.MENU);

        assertEquals("Obsolete keys found in the menu properties file: " + obsoleteKeys + "\n" +
                "1. CHECK IF THE KEY IS REALLY NOT USED ANYMORE\n" +
                "2. REMOVE THESE FROM THE ENGLISH MENU FILE\n" +
                "3. EXECUTE: gradlew localizationUpdate\n",
                Collections.<String>emptyList(), obsoleteKeys);
    }

}
