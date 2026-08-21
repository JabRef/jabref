package org.jabref.logic.ocr;

import java.util.List;

import javafx.collections.FXCollections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OcrPreferencesTest {

    private OcrPreferences preferences;

    @BeforeEach
    void setUp() {
        preferences = OcrPreferences.getDefault();
    }

    @Test
    void defaultOcrLanguagesIsOnlyEnglish() {
        assertEquals(List.of(OcrLanguage.ENGLISH), preferences.getOcrLanguages());
    }

    @Test
    void setOcrLanguagesUpdatesSelectedLanguages() {
        preferences.setOcrLanguages(List.of(OcrLanguage.GERMAN, OcrLanguage.FRENCH));
        assertEquals(List.of(OcrLanguage.GERMAN, OcrLanguage.FRENCH), preferences.getOcrLanguages());
    }

    @Test
    void ocrLanguagesPropertyExposesSameUnderlyingValue() {
        preferences.setOcrLanguages(List.of(OcrLanguage.SIMPLIFIED_CHINESE));
        assertEquals(List.of(OcrLanguage.SIMPLIFIED_CHINESE), preferences.ocrLanguagesProperty().get());
    }

    @Test
    void orderingIsPreserved() {
        List<OcrLanguage> orderedLanguages = List.of(OcrLanguage.ENGLISH, OcrLanguage.JAPANESE, OcrLanguage.SPANISH);
        preferences.setOcrLanguages(orderedLanguages);
        assertEquals(orderedLanguages, preferences.getOcrLanguages());
    }
}
