package org.jabref.logic.l10n;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LanguageTest {

    @Test
    void convertKnownLanguageOnly() {
        assertEquals(Optional.of(new Locale("en")), Language.convertToSupportedLocale(Language.ENGLISH));
    }

    @Test
    void convertKnownLanguageAndCountryCorrect() {
        // Language and country code have to be separated see: https://stackoverflow.com/a/3318598
        assertEquals(Optional.of(new Locale("pt", "BR")), Language.convertToSupportedLocale(Language.BRAZILIAN_PORTUGUESE));
    }

    @Test
    void convertToKnownLocaleNull() {
        assertThrows(NullPointerException.class, () -> Language.convertToSupportedLocale(null));
    }
}
