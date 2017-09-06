package org.jabref.logic.l10n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalizationConsistencyTest {

    @Test
    public void allFilesMustBeInLanguages() throws IOException {
        for (String bundle : Arrays.asList("JabRef", "Menu")) {
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
            Assert.assertEquals("There are some localization files that are not present in org.jabref.logic.l10n.Languages or vice versa!",
                    Collections.<String>emptySet(), Sets.symmetricDifference(new HashSet<>(Languages.LANGUAGES.values()), localizationFiles));
        }
    }

    @Test
    public void allFilesMustHaveSameKeys() {
        for (String bundle : Arrays.asList("JabRef", "Menu")) {
            Set<String> englishKeys = LocalizationParser
                    .getKeysInPropertiesFile(String.format("/l10n/%s_%s.properties", bundle, "en"));

            List<String> nonEnglishLanguages = Languages.LANGUAGES.values().stream().filter(l -> !"en".equals(l))
                    .collect(Collectors.toList());
            for (String lang : nonEnglishLanguages) {
                Set<String> nonEnglishKeys = LocalizationParser
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
        List<LocalizationEntry> missingKeys = LocalizationParser.find(LocalizationBundleForTest.LANG).stream().sorted()
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
        Set<LocalizationEntry> missingKeys = LocalizationParser.find(LocalizationBundleForTest.MENU);

        assertEquals("DETECTED LANGUAGE KEYS WHICH ARE NOT IN THE ENGLISH MENU FILE\n" +
                        "1. PASTE THESE INTO THE ENGLISH MENU FILE\n" +
                        "2. EXECUTE: gradlew localizationUpdate\n" +
                        missingKeys.parallelStream()
                                .map(key -> String.format("%s=%s", key.getKey(), key.getKey()))
                                .collect(Collectors.toList()),
                Collections.<LocalizationEntry>emptySet(), missingKeys);
    }

    @Test
    public void findObsoleteLocalizationKeys() throws IOException {
        Set<String> obsoleteKeys = LocalizationParser.findObsolete(LocalizationBundleForTest.LANG);

        assertEquals("Obsolete keys found in language properties file: " + obsoleteKeys + "\n" +
                        "1. CHECK IF THE KEY IS REALLY NOT USED ANYMORE\n" +
                        "2. REMOVE THESE FROM THE ENGLISH LANGUAGE FILE\n" +
                        "3. EXECUTE: gradlew localizationUpdate\n",
                Collections.<String>emptySet(), obsoleteKeys);
    }

    @Test
    public void findObsoleteMenuLocalizationKeys() throws IOException {
        Set<String> obsoleteKeys = LocalizationParser.findObsolete(LocalizationBundleForTest.MENU);

        assertEquals("Obsolete keys found in the menu properties file: " + obsoleteKeys + "\n" +
                        "1. CHECK IF THE KEY IS REALLY NOT USED ANYMORE\n" +
                        "2. REMOVE THESE FROM THE ENGLISH MENU FILE\n" +
                        "3. EXECUTE: gradlew localizationUpdate\n",
                Collections.<String>emptySet(), obsoleteKeys);
    }

    @Test
    public void localizationParameterMustIncludeAString() throws IOException {
        // Must start or end with "
        // Localization.lang("test"), Localization.lang("test" + var), Localization.lang(var + "test")
        // TODO: Localization.lang(var1 + "test" + var2) not covered
        // Localization.lang("Problem downloading from %1", address)
        Set<LocalizationEntry> keys = LocalizationParser.findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest.LANG);
        for (LocalizationEntry e : keys) {
            assertTrue("Illegal localization parameter found. Must include a String with potential concatenation or replacement parameters. Illegal parameter: Localization.lang(" + e.getKey(),
                    e.getKey().startsWith("\"") || e.getKey().endsWith("\""));
        }

        keys = LocalizationParser.findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest.MENU);
        for (LocalizationEntry e : keys) {
            assertTrue("Illegal localization parameter found. Must include a String with potential concatenation or replacement parameters. Illegal parameter: Localization.lang(" + e.getKey(),
                    e.getKey().startsWith("\"") || e.getKey().endsWith("\""));
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
}
