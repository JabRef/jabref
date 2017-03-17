package org.jabref.logic.l10n;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LocalizationKeyParamsTest {

    @Test
    public void testReplacePlaceholders() {
        assertEquals("biblatex mode", new LocalizationKeyParams("biblatex mode").replacePlaceholders());
        assertEquals("biblatex mode", new LocalizationKeyParams("%0 mode", "biblatex").replacePlaceholders());
        assertEquals("C:\\bla mode", new LocalizationKeyParams("%0 mode", "C:\\bla").replacePlaceholders());
        assertEquals("What \n : %e %c a b", new LocalizationKeyParams("What \n : %e %c_%0 %1", "a", "b").replacePlaceholders());
    }

    @Test(expected = IllegalStateException.class)
    public void testTooManyParams() {
        new LocalizationKeyParams("", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0");
    }

}
