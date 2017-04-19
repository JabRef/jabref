package org.jabref.logic.l10n;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LocalizationTest {

    private Locale locale;

    @Before
    public void storeDefaultLocale() {
        locale = Locale.getDefault();
    }

    @After
    public void restoreDefaultLocale() {
        Locale.setDefault(locale);
        javax.swing.JComponent.setDefaultLocale(locale);
    }

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
        assertEquals("en", Locale.getDefault().toString());
    }

    @Test
    public void testKnownTranslation() {
        Localization.setLanguage("en");
        String knownKey = "Groups";
        assertEquals(knownKey, Localization.lang(knownKey));
        String knownValueWithMnemonics = "&Groups";
        assertEquals(knownValueWithMnemonics, Localization.menuTitle(knownKey));
    }

    @Test
    public void testKnownTranslationWithCountryModifier() {
        Localization.setLanguage("en_US");
        String knownKey = "Groups";
        assertEquals(knownKey, Localization.lang(knownKey));
        String knownValueWithMnemonics = "&Groups";
        assertEquals(knownValueWithMnemonics, Localization.menuTitle(knownKey));
    }

    @Test
    public void testUnknownTranslation() {
        Localization.setLanguage("en");
        String knownKey = "WHATEVER";
        assertEquals(knownKey, Localization.lang(knownKey));
        assertEquals(knownKey, Localization.menuTitle(knownKey));
    }

    @Test
    public void testUnsetLanguageTranslation() {
        String knownKey = "Groups";
        assertEquals(knownKey, Localization.lang(knownKey));
        String knownValueWithMnemonics = "&Groups";
        assertEquals(knownValueWithMnemonics, Localization.menuTitle(knownKey));
    }

}
