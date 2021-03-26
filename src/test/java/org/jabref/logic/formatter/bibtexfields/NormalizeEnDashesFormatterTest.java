package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeEnDashesFormatterTest {

    private static final NormalizeEnDashesFormatter formatter = new NormalizeEnDashesFormatter();

    @Test
    public void formatExample() {
        assertEquals("Winery -- A Modeling Tool for TOSCA-based Cloud Applications", formatter.format(formatter.getExampleInput()));
    }

    @Test
    public void formatExampleOfChangelog() {
        assertEquals("Example -- illustrative", formatter.format("Example - illustrative"));
    }

    @Test
    public void dashesWithinWordsAreKept() {
        assertEquals("Example-illustrative", formatter.format("Example-illustrative"));
    }

    @Test
    public void dashesPreceededByASpaceAreKept() {
        assertEquals("Example -illustrative", formatter.format("Example -illustrative"));
    }

    @Test
    public void dashesFollowedByASpaceAreKept() {
        assertEquals("Example- illustrative", formatter.format("Example- illustrative"));
    }

    @Test
    public void dashAtTheBeginningIsKept() {
        assertEquals("- illustrative", formatter.format("- illustrative"));
    }

    @Test
    public void dashAtTheEndIsKept() {
        assertEquals("Example-", formatter.format("Example-"));
    }
}
