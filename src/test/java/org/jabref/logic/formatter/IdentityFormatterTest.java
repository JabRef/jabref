package org.jabref.logic.formatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class IdentityFormatterTest {

    private IdentityFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new IdentityFormatter();
    }

    @Test
    void formatExample() {
        assertEquals("JabRef", formatter.format(formatter.getExampleInput()));
    }
}
