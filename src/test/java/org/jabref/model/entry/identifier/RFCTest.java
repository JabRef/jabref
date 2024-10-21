package org.jabref.model.entry.identifier;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RFCTest {

    @Test
    void testParsePlainRfcId() {
        Optional<RFC> rfc = RFC.parse("rfc7276");
        assertTrue(rfc.isPresent());
        assertEquals("rfc7276", rfc.get().getNormalized());
    }

    @Test
    void testParseRfcUrl() {
        Optional<RFC> rfc = RFC.parse("https://www.rfc-editor.org/rfc/rfc7276.html");
        assertTrue(rfc.isPresent());
        assertEquals("rfc7276", rfc.get().getNormalized());
    }

    @Test
    void testInvalidRfc() {
        Optional<RFC> rfc = RFC.parse("invalidRfc");
        assertFalse(rfc.isPresent());
    }

    @Test
    void testGetExternalUri() {
        RFC rfc = new RFC("rfc7276");
        Optional<URI> uri = rfc.getExternalURI();
        assertTrue(uri.isPresent());
        assertEquals("https://www.rfc-editor.org/rfc/rfc7276", uri.get().toString());
    }
}
