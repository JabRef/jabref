package org.jabref.logic.l10n;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Contains all supported languages.
 */
public enum Language {

    BahasaIndonesia("Bahasa Indonesia", "in"),
    BrazilianPortuguese("Brazilian Portuguese", "pt_BR"),
    Danish("Dansk", "da"),
    German("Deutsch", "de"),
    English("English", "en"),
    Spanish("Español", "es"),
    French("Français", "fr"),
    Italian("Italiano", "it"),
    Japanese("Japanese", "ja"),
    Dutch("Nederlands", "nl"),
    Norwegian("Norsk", "no"),
    Persian("Persian (فارسی)", "fa"),
    Russian("Russian", "ru"),
    SimplifiedChinese("Simplified Chinese", "zh"),
    Svenska("Svenska", "sv"),
    Turkish("Turkish", "tr"),
    Vietnamese("Vietnamese", "vi"),
    Greek("ελληνικά", "el"),
    Tagalog("Tagalog/Filipino", "tl");

    private final String displayName;
    private final String id;

    Language(String displayName, String id) {
        this.displayName = displayName;
        this.id = id;
    }

    public static Optional<Locale> convertToSupportedLocale(Language language) {
        Objects.requireNonNull(language);

        //Very important to split languages like pt_BR into two parts, because otherwise the country would be treated lowercase and create problems in loading
        String[] languageParts = language.getId().split("_");
        Locale locale;
        if (languageParts.length == 1) {
            locale = new Locale(languageParts[0]);
        } else if (languageParts.length == 2) {
            locale = new Locale(languageParts[0], languageParts[1]);
        } else {
            locale = Locale.ENGLISH;
        }

        return Optional.of(locale);

    }

    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return id;
    }
}
