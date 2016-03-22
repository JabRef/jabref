package net.sf.jabref.logic.l10n;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class LanguagesTest {

    @Test
    public void testConvertToKnownLocale() {
        assertEquals(Optional.of("en"), Languages.convertToKnownLocale("en"));
        assertEquals(Optional.of("en"), Languages.convertToKnownLocale("en_US"));
        assertEquals(Optional.of("de"), Languages.convertToKnownLocale("de_DE"));
        assertEquals(Optional.empty(), Languages.convertToKnownLocale("WHATEVER"));
    }

    @Test(expected = NullPointerException.class)
    public void testConvertToKnownLocaleNull() {
        Languages.convertToKnownLocale(null);
    }
}