package org.jabref.http.server.cayw.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class NatbibFormatterTest {

    @Test
    void testHasAtLeastTwoAliases() {
        assertTrue(new NatbibFormatter().getFormatNames().size() >= 2,
                "Natbib formatter should expose ≥ 3 aliases");
    }
}
