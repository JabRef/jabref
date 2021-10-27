package org.jabref.logic.l10n;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Contains all supported languages.
 */
public enum Language {

    BAHASA_INDONESIA("Bahasa Indonesia", "in"),
    BRAZILIAN_PORTUGUESE("Brazilian Portuguese", "pt_BR"),
    DANISH("Dansk", "da"),
    GERMAN("Deutsch", "de"),
    ENGLISH("English", "en"),
    SPANISH("Español", "es"),
    FRENCH("Français", "fr"),
    ITALIAN("Italiano", "it"),
    JAPANESE("Japanese", "ja"),
    DUTCH("Nederlands", "nl"),
    NORWEGIAN("Norsk", "no"),
    PERSIAN("Persian (فارسی)", "fa"),
    PORTUGUESE("Português", "pt"),
    RUSSIAN("Russian", "ru"),
    SIMPLIFIED_CHINESE("Chinese (Simplified)", "zh_CN"),
    TRADITIONAL_CHINESE("Chinese (Traditional)", "zh_TW"),
    SVENSKA("Svenska", "sv"),
    TURKISH("Turkish", "tr"),
    VIETNAMESE("Vietnamese", "vi"),
    GREEK("ελληνικά", "el"),
    TAGALOG("Tagalog/Filipino", "tl"),
    POLISH("Polish", "pl"),
    KOREAN("Korean (한국어)", "ko");

    private final String displayName;
    private final String id;

    Language(String displayName, String id) {
        this.displayName = displayName;
        this.id = id;
    }

    public static Optional<Locale> convertToSupportedLocale(Language language) {
        Objects.requireNonNull(language);

        // Very important to split languages like pt_BR into two parts, because otherwise the country would be treated lowercase and create problems in loading
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
