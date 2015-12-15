package net.sf.jabref.logic.l10n;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class LocalizationTest {

    @Test
    public void testSetKnownLanguage() {
        Locale.setDefault(Locale.CHINA);
        Localization.setLanguage("en");
        assertEquals("en", Locale.getDefault().toString());
    }

    @Test
    public void testSetUnknownLanguage() {
        Locale.setDefault(Locale.CHINA);
        Localization.setLanguage("WHATEVER");
        assertEquals(Locale.CHINA.toString(), Locale.getDefault().toString());
    }

    @Test
    public void testKnownTranslation() {
        Localization.setLanguage("en");
        String knownKey = "Groups";
        assertEquals(knownKey, Localization.lang(knownKey));
        assertEquals(knownKey, Localization.menuTitle(knownKey));
    }

    @Test
    public void testUnknownTranslation() {
        Localization.setLanguage("en");
        String knownKey = "WHATEVER";
        assertEquals(knownKey, Localization.lang(knownKey));
        assertEquals(knownKey, Localization.menuTitle(knownKey));
    }

}