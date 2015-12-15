package net.sf.jabref.logic.l10n;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class LocalizationParserTest {

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