package org.jabref.model.entry.identifier;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RFCTest {

    @ParameterizedTest
    @CsvSource({
            "rfc7276, rfc7276",
            "https://www.rfc-editor.org/rfc/rfc7276.html, rfc7276"
    })
    void testParseValidRfcIdAndUrl(String input, String expected) {
        Optional<RFC> rfc = RFC.parse(input);
        assertTrue(rfc.isPresent());
        assertEquals(expected, rfc.get().getNormalized());  // Use asString() method
    }

    @Test
    void testInvalidRfc() {
        Optional<RFC> rfc = RFC.parse("invalidRfc");
        assertEquals(Optional.empty(), rfc);  // Replace assertFalse with assertEquals
    }

    @Test
    void testGetExternalUri() {
        RFC rfc = new RFC("rfc7276");
        assertEquals(Optional.of(URI.create("https://www.rfc-editor.org/rfc/rfc7276")), rfc.getExternalURI());  // Use Optional.of for expected value
    }
}
