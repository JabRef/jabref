package org.jabref.logic.ocr;

import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum OcrLanguage {
    ARABIC("ara"),
    CHINESE_SIMPLIFIED("chi_sim"),
    CHINESE_TRADITIONAL("chi_tra"),
    ENGLISH("eng"),
    FRENCH("fra"),
    GERMAN("deu"),
    ITALIAN("ita"),
    JAPANESE("jpn"),
    KOREAN("kor"),
    PORTUGUESE("por"),
    RUSSIAN("rus"),
    SPANISH("spa");

    private final String code;

    OcrLanguage(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return switch (this) {
            case ARABIC ->
                    Localization.lang("Arabic");
            case CHINESE_SIMPLIFIED ->
                    Localization.lang("Chinese (Simplified)");
            case CHINESE_TRADITIONAL ->
                    Localization.lang("Chinese (Traditional)");
            case ENGLISH ->
                    Localization.lang("English");
            case FRENCH ->
                    Localization.lang("French");
            case GERMAN ->
                    Localization.lang("German");
            case ITALIAN ->
                    Localization.lang("Italian");
            case JAPANESE ->
                    Localization.lang("Japanese");
            case KOREAN ->
                    Localization.lang("Korean");
            case PORTUGUESE ->
                    Localization.lang("Portuguese");
            case RUSSIAN ->
                    Localization.lang("Russian");
            case SPANISH ->
                    Localization.lang("Spanish");
        };
    }

    public static OcrLanguage fromCode(String code) {
        for (OcrLanguage lang : values()) {
            if (lang.code.equals(code)) {
                return lang;
            }
        }
        return ENGLISH;
    }
}
