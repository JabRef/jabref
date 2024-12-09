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
    void setKnownLanguage() {
        Locale.setDefault(Locale.CHINA);
        Localization.setLanguage(Language.ENGLISH);
        assertEquals("en", Locale.getDefault().toString());
    }

    @Test
    void knownTranslationWithGroups() {
        Localization.setLanguage(Language.ENGLISH);
        assertEquals("Groups", Localization.lang("Groups"));
    }

    @Test
    void knownEnglishTranslationOfUndo() {
        Localization.setLanguage(Language.ENGLISH);
        assertEquals("Undo", Localization.lang("Undo"));
    }

    @Test
    void knownGermanTranslation() {
        Localization.setLanguage(Language.GERMAN);
        assertEquals("Zeige Einstellungen", Localization.lang("Show preferences"));
    }

    @Test
    void newLineIsAvailableAndKeptUnescaped() {
        Localization.setLanguage(Language.ENGLISH);
        assertEquals("Hint: To search specific fields only, enter for example:\n<tt>author=smith and title=electrical</tt>", Localization.lang("Hint: To search specific fields only, enter for example:\n<tt>author=smith and title=electrical</tt>"));
    }

    @Test
    void knownTranslationWithCountryModifier() {
        Localization.setLanguage(Language.BRAZILIAN_PORTUGUESE);
        assertEquals("Grupos", Localization.lang("Groups"));
    }

    @Test
    void unknownTranslation() {
        Localization.setLanguage(Language.ENGLISH);
        assertEquals("WHATEVER", Localization.lang("WHATEVER"));
    }

    @Test
    void unsetLanguageTranslation() {
        assertEquals("Groups", Localization.lang("Groups"));
    }

    @Test
    void placeholderIsKeptWhenNoParameter() {
        // This behavior is required when %0 should be transformed to a hyperlink in the UI.
        assertEquals("Groups %0", Localization.lang("Groups %0"));
    }
}
