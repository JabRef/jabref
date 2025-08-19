package org.jabref.http.server.cayw.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TypstFormatterTest {

    @Test
    void testFirstAliasIsTypst() {
        assertEquals("typst", new TypstFormatter().getFormatNames().get(0));
    }
}
