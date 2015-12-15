package net.sf.jabref.logic.l10n;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class LocalizationTest {

    @Test
    public void testLocalizationKey() {
        assertEquals("test_\\:_\\=", new Localization.LocalizationKey("test : =").getPropertiesKey());
    }

    @Test
    public void testTranslation() {
        assertEquals("What \n : %e %c a b", new Localization.LocalizationKeyParams("What \n : %e %c_%0 %1", "a", "b").translate());
    }

    @Test
    public void testParamsReplacement() {
        assertEquals("BibLaTeX mode", new Localization.LocalizationKeyParams("%0 mode", "BibLaTeX").translate());
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
        System.out.println("PASTE THIS INTO THE NON-ENGLISH LANGUAGE FILES");
        StringJoiner result = new StringJoiner("\n");
        for (LocalizationEntry key : missingKeys) {
            result.add(String.format("%s=", key.getKey()));
        }
        return result.toString();
    }

    @Test
    public void testParsingCode() {
        String code = ""
                + "Localization.lang(\"one per line\")"
                + "Localization.lang(\n" +
                "            \"Copy \\\\cite{BibTeX key}\")"
                + "Localization.lang(\"two per line\") Localization.lang(\"two per line\")"
                + "Localization.lang(\"multi \" + \n"
                + "\"line\")"
                + "Localization.lang(\"one per line with var\", var)"
                + "Localization.lang(\"Search %0\", \"Springer\")"
                + "Localization.lang(\"Reset preferences (key1,key2,... or 'all')\")"
                + "Localization.lang(\"Multiple entries selected. Do you want to change the type of all these to '%0'?\")"
                + "Localization.lang(\"Run Fetcher, e.g. \\\"--fetch=Medline:cancer\\\"\");";

        List<String> expectedLanguageKeys = Arrays.asList("one_per_line", "Copy_\\cite{BibTeX_key}", "two_per_line", "two_per_line", "multi_line",
                "one_per_line_with_var", "Search_%0", "Reset_preferences_(key1,key2,..._or_'all')", "Multiple_entries_selected._Do_you_want_to_change_" +
                        "the_type_of_all_these_to_'%0'?", "Run_Fetcher,_e.g._\"--fetch\\=Medline\\:cancer\"");

        List<String> languageKeysInString = LocalizationParser.JavaLocalizationEntryParser.getLanguageKeysInString(code, LocalizationBundle.LANG);
        assertEquals(expectedLanguageKeys, languageKeysInString);
    }

}
