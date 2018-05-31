package org.jabref.logic.l10n;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalizationTest {

    private Locale locale;

    @BeforeEach
    public void storeDefaultLocale() {
        locale = Locale.getDefault();
    }

    @AfterEach
    public void restoreDefaultLocale() {
        Locale.setDefault(locale);
        javax.swing.JComponent.setDefaultLocale(locale);
        Localization.setLanguage("en");
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
    public void testKnownTranslationWithGroups() {
        Localization.setLanguage("en");
        String knownKey = "Groups";
        assertEquals(knownKey, Localization.lang(knownKey));
        String knownValueWithMnemonics = "&Groups";
        assertEquals(knownValueWithMnemonics, Localization.menuTitle(knownKey));
    }

    @Test
    public void testKnownEnglishTranslationOfUndo() {
        Localization.setLanguage("en");
        String knownKey = "Undo";
        assertEquals(knownKey, Localization.lang(knownKey));
        String knownValueWithMnemonics = "&Undo";
        assertEquals(knownValueWithMnemonics, Localization.menuTitle(knownKey));
    }

    @Test
    public void testKnownGermanTranslationDoesNotHaveAmpersand() {
        Localization.setLanguage("de");
        assertEquals("Alle speichern", Localization.lang("Save all"));
    }

    @Test
    public void testKnownGermanTranslation() {
        Localization.setLanguage("de");
        String knownKey = "Save all";
        assertEquals("Alle speichern", Localization.lang(knownKey));
        assertEquals("A&lle speichern", Localization.menuTitle(knownKey));
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
