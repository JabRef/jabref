package net.sf.jabref.logic.l10n;

import java.util.*;

public class Languages {

    public static final Map<String, String> LANGUAGES;

    static {
        LANGUAGES = new TreeMap<>();

        // LANGUAGES contains mappings for supported languages.
        LANGUAGES.put("Dansk", "da");
        LANGUAGES.put("Deutsch", "de");
        LANGUAGES.put("English", "en");
        LANGUAGES.put("Español", "es");
        LANGUAGES.put("Persian (فارسی)", "fa");
        LANGUAGES.put("Français", "fr");
        LANGUAGES.put("Bahasa Indonesia", "in");
        LANGUAGES.put("Italiano", "it");
        LANGUAGES.put("Japanese", "ja");
        LANGUAGES.put("Nederlands", "nl");
        LANGUAGES.put("Norsk", "no");
        LANGUAGES.put("Brazilian Portuguese", "pt_BR");
        LANGUAGES.put("Russian", "ru");
        LANGUAGES.put("Svenska", "sv");
        LANGUAGES.put("Turkish", "tr");
        LANGUAGES.put("Vietnamese", "vi");
        LANGUAGES.put("Simplified Chinese", "zh");
    }

    public static Optional<String> convertToKnownLocale(String language) {
        if(!LANGUAGES.values().contains(Objects.requireNonNull(language))) {
            if(language.contains("_")) {
                String lang = language.split("_")[0];
                if(LANGUAGES.values().contains(lang)) {
                    return Optional.of(lang);
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.of(language);
        }
    }
}
