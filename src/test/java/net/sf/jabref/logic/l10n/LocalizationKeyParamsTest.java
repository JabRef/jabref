package net.sf.jabref.logic.l10n;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LocalizationKeyParamsTest {

    @Test
    public void testReplacePlaceholders() {
        assertEquals("BibLaTeX mode", new LocalizationKeyParams("BibLaTeX mode").replacePlaceholders());
        assertEquals("BibLaTeX mode", new LocalizationKeyParams("%0 mode", "BibLaTeX").replacePlaceholders());
        assertEquals("C:\\bla mode", new LocalizationKeyParams("%0 mode", "C:\\bla").replacePlaceholders());
        assertEquals("What \n : %e %c a b", new LocalizationKeyParams("What \n : %e %c_%0 %1", "a", "b").replacePlaceholders());
    }

    @Test(expected = IllegalStateException.class)
    public void testTooManyParams() {
        new LocalizationKeyParams("", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0");
    }

}