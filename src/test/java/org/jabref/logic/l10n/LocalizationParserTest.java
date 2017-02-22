package org.jabref.logic.l10n;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LocalizationParserTest {

    @Test
    public void testKeyParsingCode() {
        assertLocalizationKeyParsing("Localization.lang(\"one per line\")", "one_per_line");
        assertLocalizationKeyParsing("Localization.lang(\n            \"Copy \\\\cite{BibTeX key}\")", "Copy_\\cite{BibTeX_key}");
        assertLocalizationKeyParsing("Localization.lang(\"two per line\") Localization.lang(\"two per line\")", Arrays.asList("two_per_line", "two_per_line"));
        assertLocalizationKeyParsing("Localization.lang(\"multi \" + \n\"line\")", "multi_line");
        assertLocalizationKeyParsing("Localization.lang(\"one per line with var\", var)", "one_per_line_with_var");
        assertLocalizationKeyParsing("Localization.lang(\"Search %0\", \"Springer\")", "Search_%0");
        assertLocalizationKeyParsing("Localization.lang(\"Reset preferences (key1,key2,... or 'all')\")", "Reset_preferences_(key1,key2,..._or_'all')");
        assertLocalizationKeyParsing("Localization.lang(\"Multiple entries selected. Do you want to change the type of all these to '%0'?\")",
                "Multiple_entries_selected._Do_you_want_to_change_the_type_of_all_these_to_'%0'?");
        assertLocalizationKeyParsing("Localization.lang(\"Run fetcher, e.g. \\\"--fetch=Medline:cancer\\\"\");",
                "Run_fetcher,_e.g._\"--fetch\\=Medline\\:cancer\"");
    }

    @Test
    public void testParameterParsingCode() {
        assertLocalizationParameterParsing("Localization.lang(\"one per line\")", "\"one per line\"");
        assertLocalizationParameterParsing("Localization.lang(\"one per line\" + var)", "\"one per line\" + var");
        assertLocalizationParameterParsing("Localization.lang(var + \"one per line\")", "var + \"one per line\"");
        assertLocalizationParameterParsing("Localization.lang(\"Search %0\", \"Springer\")", "\"Search %0\", \"Springer\"");
    }

    private void assertLocalizationKeyParsing(String code, String expectedLanguageKeys) {
        assertLocalizationKeyParsing(code, Collections.singletonList(expectedLanguageKeys));
    }

    private void assertLocalizationKeyParsing(String code, List<String> expectedLanguageKeys) {
        List<String> languageKeysInString = LocalizationParser.JavaLocalizationEntryParser.getLanguageKeysInString(code, LocalizationBundleForTest.LANG);
        assertEquals(expectedLanguageKeys, languageKeysInString);
    }

    private void assertLocalizationParameterParsing(String code, List<String> expectedParameter) {
        List<String> languageKeysInString = LocalizationParser.JavaLocalizationEntryParser.getLocalizationParameter(code, LocalizationBundleForTest.LANG);
        assertEquals(expectedParameter, languageKeysInString);
    }

    private void assertLocalizationParameterParsing(String code, String expectedParameter) {
        assertLocalizationParameterParsing(code, Collections.singletonList(expectedParameter));
    }

}
