package net.sf.jabref.logic.l10n;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class LocalizationTest {

    @BeforeClass @AfterClass
    public static void loadTranslationDefaultLocale() {
        System.out.println(Locale.getDefault());
    }

    @Test
    public void testSetKnownLanguage() {
        Locale before = Locale.getDefault();

        try {
            Locale.setDefault(Locale.CHINA);
            Localization.setLanguage("en");
            assertEquals("en", Locale.getDefault().toString());
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    public void testSetUnknownLanguage() {
        Locale before = Locale.getDefault();

        try {
            Locale.setDefault(Locale.CHINA);
            Localization.setLanguage("WHATEVER");
            assertEquals(Locale.CHINA.toString(), Locale.getDefault().toString());
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    public void testKnownTranslation() {
        Locale before = Locale.getDefault();

        try {
            Localization.setLanguage("en");
            String knownKey = "Groups";
            assertEquals(knownKey, Localization.lang(knownKey));
            assertEquals(knownKey, Localization.menuTitle(knownKey));
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    public void testUnknownTranslation() {
        Locale before = Locale.getDefault();

        try {
            Localization.setLanguage("en");
            String knownKey = "WHATEVER";
            assertEquals(knownKey, Localization.lang(knownKey));
            assertEquals(knownKey, Localization.menuTitle(knownKey));
        } finally {
            Locale.setDefault(before);
        }
    }

}