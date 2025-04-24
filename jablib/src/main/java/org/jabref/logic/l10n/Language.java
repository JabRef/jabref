package org.jabref.logic.l10n;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

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
    FINNISH("Suomi", "fi"),
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

    private static final Pattern IS_NOT_LATIN = Pattern.compile("[^\\p{IsLatin}]");
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
            locale = Locale.of(languageParts[0]);
        } else if (languageParts.length == 2) {
            locale = Locale.of(languageParts[0], languageParts[1]);
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

    public static List<Language> getSorted() {
        return Arrays.stream(values())
                .sorted(Comparator.comparing(language -> removeNonLatinCharacters(language.getDisplayName())))
                .toList();
    }

    private static String removeNonLatinCharacters(String input) {
       return IS_NOT_LATIN.matcher(input).replaceAll("");
    }
}
