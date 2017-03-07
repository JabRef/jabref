package org.jabref.logic.l10n;

import java.util.Locale;
import java.util.Optional;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
    public void convertKnownLanguageAndCountryOnly() {
        assertEquals(Optional.of(new Locale("en")), Languages.convertToSupportedLocale("en_US"));
    }

    @Test
    public void convertKnownLanguageAndUnknownCountry() {
        assertEquals(Optional.of(new Locale("en")), Languages.convertToSupportedLocale("en_GB_unknownvariant"));
    }

    @Test
    public void convertUnknownKnownLanguageAndUnknownCountry() {
        assertEquals(Optional.empty(), Languages.convertToSupportedLocale("language_country_variant"));
    }

    @Test(expected = NullPointerException.class)
    public void convertToKnownLocaleNull() {
        Languages.convertToSupportedLocale(null);
    }
}
