package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeEmDashesIEEEFormatterTest {
    private NormalizeEmDashesIEEEFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new NormalizeEmDashesIEEEFormatter();
    }

    @Test
    public void formatExample() {
        String input = "Towards situation-aware adaptive workflows: SitOPT &amp;#x2014; A general purpose situation-aware workflow management system";
        String expected = "Towards situation-aware adaptive workflows: SitOPT — A general purpose situation-aware workflow management system";
        assertEquals(expected, formatter.format(input));
    }

    @Test
    public void formatExampleIllustrative() {
        assertEquals("Example — illustrative", formatter.format("Example &amp;#x2014; illustrative"));
    }
}
