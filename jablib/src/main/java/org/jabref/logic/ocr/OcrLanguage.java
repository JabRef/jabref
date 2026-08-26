package org.jabref.logic.ocr;

import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum OcrLanguage {
    ARABIC("ara", Localization.lang("Arabic")),
    CHINESE_SIMPLIFIED("chi_sim", Localization.lang("Chinese (Simplified)")),
    CHINESE_TRADITIONAL("chi_tra", Localization.lang("Chinese (Traditional)")),
    ENGLISH("eng", Localization.lang("English")),
    FRENCH("fra", Localization.lang("French")),
    GERMAN("deu", Localization.lang("German")),
    ITALIAN("ita", Localization.lang("Italian")),
    JAPANESE("jpn", Localization.lang("Japanese")),
    KOREAN("kor", Localization.lang("Korean")),
    PORTUGUESE("por", Localization.lang("Portuguese")),
    RUSSIAN("rus", Localization.lang("Russian")),
    SPANISH("spa", Localization.lang("Spanish"));

    private final String code;
    private final String displayName;

    OcrLanguage(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static OcrLanguage safeValueOf(String code) {
        for (OcrLanguage lang : values()) {
            if (lang.code.equals(code)) {
                return lang;
            }
        }
        return ENGLISH;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
