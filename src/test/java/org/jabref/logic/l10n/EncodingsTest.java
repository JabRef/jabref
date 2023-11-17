package org.jabref.logic.l10n;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class EncodingsTest {

    @Test
    public void charsetsShouldNotBeNull() {
        assertNotNull(Encodings.ENCODINGS);
    }

    @Test
    public void displayNamesShouldNotBeNull() {
        assertNotNull(Encodings.ENCODINGS_DISPLAYNAMES);
    }

    @Test
    public void charsetsShouldNotBeEmpty() {
        assertNotEquals(0, Encodings.ENCODINGS.length);
    }

    @Test
    public void displayNamesShouldNotBeEmpty() {
        assertNotEquals(0, Encodings.ENCODINGS_DISPLAYNAMES.length);
    }
}
