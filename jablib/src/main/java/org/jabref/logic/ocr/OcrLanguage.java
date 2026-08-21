package org.jabref.logic.ocr;

import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Supported OCR languages and their corresponding Tesseract / OCRmyPDF language codes.
@NullMarked
public enum OcrLanguage {
    ARABIC("العربية (Arabic)", "ara"),
    BAHASA_INDONESIA("Bahasa Indonesia", "ind"),
    DANISH("Dansk", "dan"),
    DUTCH("Nederlands", "nld"),
    ENGLISH("English", "eng"),
    FINNISH("Suomi", "fin"),
    FRENCH("Français", "fra"),
    GERMAN("Deutsch", "deu"),
    GREEK("ελληνικά (Greek)", "ell"),
    ITALIAN("Italiano", "ita"),
    JAPANESE("Japanese", "jpn"),
    KOREAN("한국어 (Korean)", "kor"),
    NORWEGIAN("Norsk", "nor"),
    PERSIAN("فارسی (Farsi)", "fas"),
    POLISH("Polish", "pol"),
    PORTUGUESE("Português", "por"),
    RUSSIAN("Russian", "rus"),
    SIMPLIFIED_CHINESE("简体中文 (Chinese Simplified)", "chi_sim"),
    SPANISH("Español", "spa"),
    SWEDISH("Svenska", "swe"),
    TAGALOG("Tagalog/Filipino", "tgl"),
    TRADITIONAL_CHINESE("Chinese (Traditional)", "chi_tra"),
    TURKISH("Turkish", "tur"),
    UKRAINIAN("украї́нська (Ukrainian)", "ukr"),
    VIETNAMESE("Vietnamese", "vie");

    private final String displayName;
    private final String tesseractCode;

    OcrLanguage(String displayName, String tesseractCode) {
        this.displayName = displayName;
        this.tesseractCode = tesseractCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTesseractCode() {
        return tesseractCode;
    }

    /// Safely finds an [OcrLanguage] from a Tesseract language code (case-insensitive).
    ///
    /// @param code the Tesseract language code (e.g. "eng", "deu")
    /// @return the matching [OcrLanguage], or [Optional#empty()] if not recognized.
    public static Optional<OcrLanguage> fromTesseractCode(@Nullable String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Stream.of(values())
                     .filter(language -> language.tesseractCode.equalsIgnoreCase(code.trim()))
                     .findFirst();
    }
}
