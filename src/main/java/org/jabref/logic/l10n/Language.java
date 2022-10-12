package org.jabref.logic.l10n;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Contains all supported languages.
 */
public enum Language {
    ARABIC("العربية (Arabic)", "ar"),
    BAHASA_INDONESIA("Bahasa Indonesia", "id"),
    BRAZILIAN_PORTUGUESE("Brazilian Portuguese", "pt_BR"),
    DANISH("Dansk", "da"),
    DUTCH("Nederlands", "nl"),
    ENGLISH("English", "en"),
    FRENCH("Français", "fr"),
    GERMAN("Deutsch", "de"),
    GREEK("ελληνικά (Greek)", "el"),
    ITALIAN("Italiano", "it"),
    JAPANESE("Japanese", "ja"),
    KOREAN("한국어 (Korean)", "ko"),
    NORWEGIAN("Norsk", "no"),
    PERSIAN("فارسی (Farsi)", "fa"),
    POLISH("Polish", "pl"),
    PORTUGUESE("Português", "pt"),
    RUSSIAN("Russian", "ru"),
    SIMPLIFIED_CHINESE("Chinese (Simplified)", "zh_CN"),
    SPANISH("Español", "es"),
    SWEDISH("Svenska", "sv"),
    TAGALOG("Tagalog/Filipino", "tl"),
    TRADITIONAL_CHINESE("Chinese (Traditional)", "zh_TW"),
    TURKISH("Turkish", "tr"),
    UKRAINIAN("украї́нська (Ukrainian)", "uk"),
    VIETNAMESE("Vietnamese", "vi");

    private final String displayName;
    private final String id;

    /**
     * @param id Typically as 639-1 code
     */
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
