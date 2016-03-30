package net.sf.jabref.logic.l10n;

import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class LocalizationConsistencyTest {

    @Test
    public void allFilesMustHaveSameKeys() {
        for (String bundle : Arrays.asList("JabRef", "Menu")) {
            List<String> englishKeys = LocalizationParser.getKeysInPropertiesFile(String.format("/l10n/%s_%s.properties", bundle, "en"));

            List<String> nonEnglishLanguages = Languages.LANGUAGES.values().stream().filter(l -> !"en".equals(l)).collect(Collectors.toList());
            for (String lang : nonEnglishLanguages) {
                List<String> nonEnglishKeys = LocalizationParser.getKeysInPropertiesFile(String.format("/l10n/%s_%s.properties", bundle, lang));

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
    public void keyValueShouldBeEqualForEnglishPropertiesMenu() {
        Properties englishKeys = LocalizationParser.getProperties(String.format("/l10n/%s_%s.properties", "Menu", "en"));
        for(Map.Entry<Object, Object> entry : englishKeys.entrySet()) {
            String expectedKeyEqualsKey = String.format("%s=%s", entry.getKey(), entry.getKey());
            String actualKeyEqualsValue = String.format("%s=%s", entry.getKey(), entry.getValue().toString().replace("&", ""));
            assertEquals(expectedKeyEqualsKey, actualKeyEqualsValue);
        }
    }

    @Test
    public void keyValueShouldBeEqualForEnglishPropertiesMessages() {
        Properties englishKeys = LocalizationParser.getProperties(String.format("/l10n/%s_%s.properties", "JabRef", "en"));
        for(Map.Entry<Object, Object> entry : englishKeys.entrySet()) {
            String expectedKeyEqualsKey = String.format("%s=%s", entry.getKey(), entry.getKey());
            String actualKeyEqualsValue = String.format("%s=%s", entry.getKey(), entry.getValue());
            assertEquals(expectedKeyEqualsKey, actualKeyEqualsValue);
        }
    }

    @Test
    public void findMissingLocalizationKeys() throws IOException {
        List<LocalizationEntry> missingKeys = LocalizationParser.find(LocalizationBundle.LANG).stream().sorted().distinct().collect(Collectors.toList());

        printInfos(missingKeys);

        String resultString = missingKeys.stream().map(Object::toString).collect(Collectors.joining("\n"));
        assertEquals("source code contains language keys for the messages which are not in the corresponding properties file", "", resultString);
    }

    @Test
    public void findMissingMenuLocalizationKeys() throws IOException {
        List<LocalizationEntry> missingKeys = LocalizationParser.find(LocalizationBundle.MENU).stream().collect(Collectors.toList());

        printInfos(missingKeys);

        String resultString = missingKeys.stream().map(Object::toString).collect(Collectors.joining("\n"));
        assertEquals("source code contains language keys for the menu which are not in the corresponding properties file",
                "", resultString);
    }

    @Test
    public void findObsoleteLocalizationKeys() throws IOException {
        List<String> obsoleteKeys = LocalizationParser.findObsolete(LocalizationBundle.LANG);

        if (!obsoleteKeys.isEmpty()) {
            System.out.println();
            System.out.println("Obsolete keys found:");
            System.out.println(obsoleteKeys.stream().map(Object::toString).collect(Collectors.joining("\n")));
            System.out.println();
            System.out.println("1. REMOVE THESE FROM THE ENGLISH LANGUAGE FILE");
            System.out.println(
                    "2. EXECUTE gradlew -b localization.gradle compareAndUpdateTranslationsWithEnglishTranslation TO");
            System.out.println("REMOVE THESE FROM THE NON-ENGLISH LANGUAGE FILES");
            fail("Obsolete keys found in properties file which should be removed");
        }
    }

    @Test
    public void findObsoleteMenuLocalizationKeys() throws IOException {
        List<String> obsoleteKeys = LocalizationParser.findObsolete(LocalizationBundle.MENU);

        if (!obsoleteKeys.isEmpty()) {
            System.out.println();
            System.out.println("Obsolete menu keys found:");
            System.out.println(obsoleteKeys.stream().map(Object::toString).collect(Collectors.joining("\n")));
            System.out.println();
            System.out.println("1. REMOVE THESE FROM THE ENGLISH LANGUAGE FILE");
            System.out.println(
                    "2. EXECUTE gradlew -b localization.gradle compareAndUpdateTranslationsWithEnglishTranslation TO");
            System.out.println("REMOVE THESE FROM THE NON-ENGLISH LANGUAGE FILES");
            fail("Obsolete keys found in menu properties file which should be removed");
        }
    }

    private void printInfos(List<LocalizationEntry> missingKeys) {
        if (!missingKeys.isEmpty()) {
            System.out.println(convertToEnglishPropertiesFile(missingKeys));
            System.out.println();
            System.out.println();
            System.out.println(convertPropertiesFile(missingKeys));
        }
    }

    private String convertToEnglishPropertiesFile(List<LocalizationEntry> missingKeys) {
        System.out.println("PASTE THIS INTO THE ENGLISH LANGUAGE FILE");
        StringJoiner result = new StringJoiner("\n");
        for (LocalizationEntry key : missingKeys) {
            result.add(String.format("%s=%s", key.getKey(), key.getKey()));
        }
        return result.toString();
    }

    private String convertPropertiesFile(List<LocalizationEntry> missingKeys) {
        System.out.println(
                "EXECUTE gradlew -b localization.gradle compareAndUpdateTranslationsWithEnglishTranslation TO");
        System.out.println("PASTE THIS INTO THE NON-ENGLISH LANGUAGE FILES");
        StringJoiner result = new StringJoiner("\n");
        for (LocalizationEntry key : missingKeys) {
            result.add(String.format("%s=", key.getKey()));
        }
        return result.toString();
    }

}
