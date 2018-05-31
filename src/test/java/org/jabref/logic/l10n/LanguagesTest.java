package org.jabref.logic.l10n;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LanguagesTest {

    @Test
    public void convertKnownLanguageOnly() {
        assertEquals(Optional.of(new Locale("en")), Languages.convertToSupportedLocale("en"));
    }

    @Test
    public void convertUnknownLanguage() {
        assertEquals(Optional.empty(), Languages.convertToSupportedLocale("This is not a locale"));
    }

    @Test
    public void convertKnownLanguageAndCountryCorrect() {
        //Language and country code have to be separated see: https://stackoverflow.com/a/3318598
        assertEquals(Optional.of(new Locale("pt", "BR")), Languages.convertToSupportedLocale("pt_BR"));
    }

    @Test
    public void convertKnownLanguageAndCountryInCorrect() {
        //Language and country code have to be separated see: https://stackoverflow.com/a/3318598
        assertFalse(Optional.of(new Locale("pt_BR")).equals(Languages.convertToSupportedLocale("pt_BR")));
    }

    @Test
    public void convertKnownLanguageAndCountryOnly() {
        assertEquals(Optional.empty(), Languages.convertToSupportedLocale("en_US"));
    }

    @Test
    public void convertKnownLanguageAndUnknownCountry() {
        assertEquals(Optional.empty(), Languages.convertToSupportedLocale("en_GB_unknownvariant"));
    }

    @Test
    public void convertUnknownKnownLanguageAndUnknownCountry() {
        assertEquals(Optional.empty(), Languages.convertToSupportedLocale("language_country_variant"));
    }

    @Test
    public void convertToKnownLocaleNull() {
        assertThrows(NullPointerException.class, () -> Languages.convertToSupportedLocale(null));
    }
}
