package org.jabref.logic.l10n;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalizationKeyTest {

    @Test
    public void testConversionToPropertiesKey() {
        LocalizationKey localizationKey = new LocalizationKey("#test! : =");
        assertEquals("\\#test\\!\\ \\:\\ \\=", localizationKey.getPropertiesKey());
        assertEquals("#test! : =", localizationKey.getPropertiesKeyUnescaped());
        assertEquals("#test! : =", localizationKey.getTranslationValue());
    }

    @Test
    public void underscoreIsPreserved() {
        LocalizationKey localizationKey = new LocalizationKey("test_with_underscore");
        assertEquals("test_with_underscore", localizationKey.getPropertiesKey());
    }

}
