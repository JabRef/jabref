package org.jabref.logic.l10n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizationTest {

    private Locale locale;

    @BeforeEach
    void storeDefaultLocale() {
        locale = Locale.getDefault();
    }

    @AfterEach
    void restoreDefaultLocale() {
        Locale.setDefault(locale);
        javax.swing.JComponent.setDefaultLocale(locale);
        Localization.setLanguage("en");
    }

    @Test
    void testSetKnownLanguage() {
        Locale.setDefault(Locale.CHINA);
        Localization.setLanguage("en");
        assertEquals("en", Locale.getDefault().toString());
    }

    @Test
    void testSetUnknownLanguage() {
        Locale.setDefault(Locale.CHINA);
        Localization.setLanguage("WHATEVER");
        assertEquals("en", Locale.getDefault().toString());
    }

    @Test
    void testKnownTranslationWithGroups() {
        Localization.setLanguage("en");
        assertEquals("Groups", Localization.lang("Groups"));
    }

    @Test
    void testKnownEnglishTranslationOfUndo() {
        Localization.setLanguage("en");
        assertEquals("Undo", Localization.lang("Undo"));
    }

    @Test
    void testKnownGermanTranslation() {
        Localization.setLanguage("de");
        assertEquals("Alle speichern", Localization.lang("Save all"));
    }

    @Test
    void testKnownTranslationWithCountryModifier() {
        Localization.setLanguage("en_US");
        assertEquals("Groups", Localization.lang("Groups"));
    }

    @Test
    void testUnknownTranslation() {
        Localization.setLanguage("en");
        assertEquals("WHATEVER", Localization.lang("WHATEVER"));
    }

    @Test
    void testUnsetLanguageTranslation() {
        assertEquals("Groups", Localization.lang("Groups"));
    }

}
