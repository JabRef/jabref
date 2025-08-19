package org.jabref.http.server.cayw.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class LatexFormatterTest {

    @Test
    void testGetFormatNamesNotEmpty() {
        LatexFormatter f = new LatexFormatter();
        assertFalse(f.getFormatNames().isEmpty(), "Format-name list must not be empty");
    }

    @Test
    void testMediaTypeIsTextPlain() {
        assertEquals("text/plain", new LatexFormatter().getMediaType().toString());
    }
}
