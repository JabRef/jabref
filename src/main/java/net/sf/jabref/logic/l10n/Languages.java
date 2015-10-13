package net.sf.jabref.logic.l10n;

import java.util.Map;
import java.util.TreeMap;

public class Languages {

    public static final Map<String, String> LANGUAGES;

    static {
        LANGUAGES = new TreeMap<>();

        // LANGUAGES contains mappings for supported languages.
        LANGUAGES.put("Dansk", "da");
        LANGUAGES.put("Deutsch", "de");
        LANGUAGES.put("English", "en");
        LANGUAGES.put("Español", "es");
        LANGUAGES.put("Persian (?????)", "fa");
        LANGUAGES.put("Fran\u00E7ais", "fr");
        LANGUAGES.put("Bahasa Indonesia", "in");
        LANGUAGES.put("Italiano", "it");
        LANGUAGES.put("Japanese", "ja");
        LANGUAGES.put("Nederlands", "nl");
        LANGUAGES.put("Norsk", "no");
        LANGUAGES.put("Brazilian Portugese", "pt_BR");
        LANGUAGES.put("Russian", "ru");
        LANGUAGES.put("Turkish", "tr");
        LANGUAGES.put("Vietnamese", "vi");
        LANGUAGES.put("Simplified Chinese", "zh");
    }

}
