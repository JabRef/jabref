package net.sf.jabref.logic.l10n;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LocalizationKeyParamsTest {

    @Test
    public void testReplacePlaceholders() throws Exception {
        assertEquals("BibLaTeX mode", new LocalizationKeyParams("%0 mode", "BibLaTeX").replacePlaceholders());
        assertEquals("What \n : %e %c a b", new LocalizationKeyParams("What \n : %e %c_%0 %1", "a", "b").replacePlaceholders());
    }

}