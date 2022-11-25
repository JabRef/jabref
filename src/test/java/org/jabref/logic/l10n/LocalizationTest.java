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
        Localization.setLanguage(Language.ENGLISH);
    }

    @Test
    void testSetKnownLanguage() {
        Locale.setDefault(Locale.CHINA);
        Localization.setLanguage(Language.ENGLISH);
        assertEquals("en", Locale.getDefault().toString());
    }

    @Test
    void testKnownTranslationWithGroups() {
        Localization.setLanguage(Language.ENGLISH);
        assertEquals("Groups", Localization.lang("Groups"));
    }

    @Test
    void testKnownEnglishTranslationOfUndo() {
        Localization.setLanguage(Language.ENGLISH);
        assertEquals("Undo", Localization.lang("Undo"));
    }

    @Test
    void testKnownGermanTranslation() {
        Localization.setLanguage(Language.GERMAN);
        assertEquals("Zeige Einstellungen", Localization.lang("Show preferences"));
    }

    @Test
    void newLineIsAvailableAndKeptUnescaped() {
        Localization.setLanguage(Language.ENGLISH);
        assertEquals("Hint: To search specific fields only, enter for example:\n<tt>author=smith and title=electrical</tt>", Localization.lang("Hint: To search specific fields only, enter for example:\n<tt>author=smith and title=electrical</tt>"));
    }

    @Test
    void testKnownTranslationWithCountryModifier() {
        Localization.setLanguage(Language.BRAZILIAN_PORTUGUESE);
        assertEquals("Grupos", Localization.lang("Groups"));
    }

    @Test
    void testUnknownTranslation() {
        Localization.setLanguage(Language.ENGLISH);
        assertEquals("WHATEVER", Localization.lang("WHATEVER"));
    }

    @Test
    void testUnsetLanguageTranslation() {
        assertEquals("Groups", Localization.lang("Groups"));
    }
}
