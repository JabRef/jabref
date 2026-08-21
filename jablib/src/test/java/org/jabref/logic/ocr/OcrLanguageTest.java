package org.jabref.logic.ocr;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrLanguageTest {

    @ParameterizedTest
    @CsvSource({
            "eng, ENGLISH, English",
            "deu, GERMAN, Deutsch",
            "fra, FRENCH, Français",
            "spa, SPANISH, Español",
            "jpn, JAPANESE, Japanese",
            "kor, KOREAN, 한국어 (Korean)",
            "chi_sim, SIMPLIFIED_CHINESE, 简体中文 (Chinese Simplified)",
            "chi_tra, TRADITIONAL_CHINESE, Chinese (Traditional)",
            "por, PORTUGUESE, Português",
            "ara, ARABIC, العربية (Arabic)"
    })
    void fromKnownTesseractCode(String code, OcrLanguage expectedLanguage, String expectedDisplayName) {
        Optional<OcrLanguage> result = OcrLanguage.fromTesseractCode(code);
        assertTrue(result.isPresent());
        assertEquals(expectedLanguage, result.get());
        assertEquals(code, result.get().getTesseractCode());
        assertEquals(expectedDisplayName, result.get().getDisplayName());
    }

    @Test
    void caseInsensitiveLookup() {
        assertEquals(Optional.of(OcrLanguage.ENGLISH), OcrLanguage.fromTesseractCode("ENG"));
        assertEquals(Optional.of(OcrLanguage.GERMAN), OcrLanguage.fromTesseractCode("DeU"));
        assertEquals(Optional.of(OcrLanguage.SIMPLIFIED_CHINESE), OcrLanguage.fromTesseractCode("CHI_SIM"));
    }

    @Test
    void lookupWithWhitespace() {
        assertEquals(Optional.of(OcrLanguage.ENGLISH), OcrLanguage.fromTesseractCode("  eng  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "en", "de", "invalid_code", "123"})
    void fromUnknownCodeReturnsEmpty(String invalidCode) {
        assertEquals(Optional.empty(), OcrLanguage.fromTesseractCode(invalidCode));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void fromNullOrBlankCodeReturnsEmpty(String blankCode) {
        assertEquals(Optional.empty(), OcrLanguage.fromTesseractCode(blankCode));
    }

    @Test
    void allLanguagesHaveNonEmptyTesseractCodeAndDisplayName() {
        for (OcrLanguage language : OcrLanguage.values()) {
            assertFalse(language.getTesseractCode().isBlank());
            assertFalse(language.getDisplayName().isBlank());
            assertEquals(Optional.of(language), OcrLanguage.fromTesseractCode(language.getTesseractCode()));
        }
    }
}
