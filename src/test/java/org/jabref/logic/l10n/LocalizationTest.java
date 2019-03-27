package org.jabref.logic.l10n;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        Localization.setLanguage(Language.English);
    }

    @Test
    void testSetKnownLanguage() {
        Locale.setDefault(Locale.CHINA);
        Localization.setLanguage(Language.English);
        assertEquals("en", Locale.getDefault().toString());
    }

    @Test
    void testKnownTranslationWithGroups() {
        Localization.setLanguage(Language.English);
        assertEquals("Groups", Localization.lang("Groups"));
    }

    @Test
    void testKnownEnglishTranslationOfUndo() {
        Localization.setLanguage(Language.English);
        assertEquals("Undo", Localization.lang("Undo"));
    }

    @Test
    void testKnownGermanTranslation() {
        Localization.setLanguage(Language.German);
        assertEquals("Zeige Einstellungen", Localization.lang("Show preferences"));
    }

    @Test
    void testKnownTranslationWithCountryModifier() {
        Localization.setLanguage(Language.BrazilianPortuguese);
        assertEquals("Grupos", Localization.lang("Groups"));
    }

    @Test
    void testUnknownTranslation() {
        Localization.setLanguage(Language.English);
        assertEquals("WHATEVER", Localization.lang("WHATEVER"));
    }

    @Test
    void testUnsetLanguageTranslation() {
        assertEquals("Groups", Localization.lang("Groups"));
    }
}
