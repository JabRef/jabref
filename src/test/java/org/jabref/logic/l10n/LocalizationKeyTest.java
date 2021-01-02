package org.jabref.logic.l10n;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizationKeyTest {

    @Test
    void testConversionToPropertiesKey() {
        LocalizationKey localizationKey = new LocalizationKey("#test! : =");
        assertEquals("\\#test\\!\\ \\:\\ \\=", localizationKey.getPropertiesKey());
        assertEquals("#test! : =", localizationKey.getPropertiesKeyUnescaped());
        assertEquals("#test! : =", localizationKey.getTranslationValue());
    }

    @Test
    void underscoreIsPreserved() {
        LocalizationKey localizationKey = new LocalizationKey("test_with_underscore");
        assertEquals("test_with_underscore", localizationKey.getPropertiesKey());
    }

    @Test
    void backslashEscaped() {
        LocalizationKey localizationKey = new LocalizationKey("Copy \\cite{citation key}");
        assertEquals("Copy\\ \\\\cite{citation\\ key}", localizationKey.getPropertiesKey());
    }
}
