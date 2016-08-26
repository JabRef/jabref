package net.sf.jabref.logic.l10n;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    public static Optional<Locale> convertToSupportedLocale(String language) {
        Objects.requireNonNull(language);

        if (!LANGUAGES.values().contains(language)) {
            if (!language.contains("_")) {
                return Optional.empty();
            }

            String lang = language.split("_")[0];
            if (!LANGUAGES.values().contains(lang)) {
                return Optional.empty();
            }
            return Optional.of(new Locale(lang));
        }

        return Optional.of(new Locale(language));
    }
}
