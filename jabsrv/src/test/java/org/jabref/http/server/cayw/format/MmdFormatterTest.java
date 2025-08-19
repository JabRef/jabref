package org.jabref.http.server.cayw.format;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class MmdFormatterTest {

    @Test
    void testGetFormatNamesNotEmpty() {
        assertFalse(new MmdFormatter().getFormatNames().isEmpty());
    }

    @Test
    void testAliasesUnique() {
        List<String> names = new MmdFormatter().getFormatNames();
        assertEquals(names.size(), names.stream().distinct().count(),
                "Aliases must be unique");
    }
}
