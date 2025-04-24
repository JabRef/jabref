package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class NormalizeEnDashesFormatterTest {

    private NormalizeEnDashesFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new NormalizeEnDashesFormatter();
    }

    @Test
    void formatExample() {
        assertEquals("Winery -- A Modeling Tool for TOSCA-based Cloud Applications", formatter.format(formatter.getExampleInput()));
    }

    @Test
    void formatExampleOfChangelog() {
        assertEquals("Example -- illustrative", formatter.format("Example - illustrative"));
    }

    @Test
    void dashesWithinWordsAreKept() {
        assertEquals("Example-illustrative", formatter.format("Example-illustrative"));
    }

    @Test
    void dashesPreceededByASpaceAreKept() {
        assertEquals("Example -illustrative", formatter.format("Example -illustrative"));
    }

    @Test
    void dashesFollowedByASpaceAreKept() {
        assertEquals("Example- illustrative", formatter.format("Example- illustrative"));
    }

    @Test
    void dashAtTheBeginningIsKept() {
        assertEquals("- illustrative", formatter.format("- illustrative"));
    }

    @Test
    void dashAtTheEndIsKept() {
        assertEquals("Example-", formatter.format("Example-"));
    }
}
