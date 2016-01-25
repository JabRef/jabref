package net.sf.jabref.logic.l10n;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class LocalizationParserTest {

    @Test
    public void testParsingCode() {
        assertLocalizationParsing("Localization.lang(\"one per line\")", "one_per_line");
        assertLocalizationParsing("Localization.lang(\n            \"Copy \\\\cite{BibTeX key}\")", "Copy_\\cite{BibTeX_key}");
        assertLocalizationParsing("Localization.lang(\"two per line\") Localization.lang(\"two per line\")", Arrays.asList("two_per_line", "two_per_line"));
        assertLocalizationParsing("Localization.lang(\"multi \" + \n\"line\")", "multi_line");
        assertLocalizationParsing("Localization.lang(\"one per line with var\", var)", "one_per_line_with_var");
        assertLocalizationParsing("Localization.lang(\"Search %0\", \"Springer\")", "Search_%0");
        assertLocalizationParsing("Localization.lang(\"Reset preferences (key1,key2,... or 'all')\")", "Reset_preferences_(key1,key2,..._or_'all')");
        assertLocalizationParsing("Localization.lang(\"Multiple entries selected. Do you want to change the type of all these to '%0'?\")",
                "Multiple_entries_selected._Do_you_want_to_change_the_type_of_all_these_to_'%0'?");
        assertLocalizationParsing("Localization.lang(\"Run Fetcher, e.g. \\\"--fetch=Medline:cancer\\\"\");",
                "Run_Fetcher,_e.g._\"--fetch\\=Medline\\:cancer\"");
    }

    private void assertLocalizationParsing(String code, String expectedLanguageKeys) {
        assertLocalizationParsing(code, Collections.singletonList(expectedLanguageKeys));
    }

    private void assertLocalizationParsing(String code, List<String> expectedLanguageKeys) {
        List<String> languageKeysInString = LocalizationParser.JavaLocalizationEntryParser.getLanguageKeysInString(code, LocalizationBundle.LANG);
        assertEquals(expectedLanguageKeys, languageKeysInString);
    }

}