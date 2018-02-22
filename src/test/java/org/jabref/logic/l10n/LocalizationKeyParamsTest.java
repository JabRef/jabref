package org.jabref.logic.l10n;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LocalizationKeyParamsTest {

    @Test
    public void testReplacePlaceholders() {
        assertEquals("biblatex mode", new LocalizationKeyParams("biblatex mode").replacePlaceholders());
        assertEquals("biblatex mode", new LocalizationKeyParams("%0 mode", "biblatex").replacePlaceholders());
        assertEquals("C:\\bla mode", new LocalizationKeyParams("%0 mode", "C:\\bla").replacePlaceholders());
        assertEquals("What \n : %e %c a b", new LocalizationKeyParams("What \n : %e %c %0 %1", "a", "b").replacePlaceholders());
        assertEquals("What \n : %e %c_a b", new LocalizationKeyParams("What \n : %e %c_%0 %1", "a", "b").replacePlaceholders());
    }

    @Test
    public void testTooManyParams() {
        assertThrows(IllegalStateException.class, () -> new LocalizationKeyParams("", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0"));
    }

}
