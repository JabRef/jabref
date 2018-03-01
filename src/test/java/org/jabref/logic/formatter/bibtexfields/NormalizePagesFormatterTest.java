package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class NormalizePagesFormatterTest {

    private NormalizePagesFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new NormalizePagesFormatter();
    }

    @Test
    public void formatSinglePageResultsInNoChange() {
        assertEquals("1", formatter.format("1"));
    }

    @Test
    public void formatPageNumbers() {
        assertEquals("1--2", formatter.format("1-2"));
    }

    @Test
    public void formatPageNumbersCommaSeparated() {
        assertEquals("1,2,3", formatter.format("1,2,3"));
    }

    @Test
    public void formatPageNumbersPlusRange() {
        assertEquals("43+", formatter.format("43+"));
    }

    @Test
    public void ignoreWhitespaceInPageNumbers() {
        assertEquals("1--2", formatter.format("   1  - 2 "));
    }

    @Test
    public void removeWhitespaceSinglePage() {
        assertEquals("1", formatter.format("   1  "));
    }

    @Test
    public void removeWhitespacePageRange() {
        assertEquals("1--2", formatter.format("   1 -- 2  "));
    }

    @Test
    public void ignoreWhitespaceInPageNumbersWithDoubleDash() {
        assertEquals("43--103", formatter.format("43 -- 103"));
    }

    @Test
    public void keepCorrectlyFormattedPageNumbers() {
        assertEquals("1--2", formatter.format("1--2"));
    }

    @Test
    public void formatPageNumbersRemoveUnexpectedLiterals() {
        assertEquals("1--2", formatter.format("{1}-{2}"));
    }

    @Test
    public void formatPageNumbersRegexNotMatching() {
        assertEquals("12", formatter.format("12"));
    }

    @Test
    public void doNotRemoveLetters() {
        assertEquals("R1-R50", formatter.format("R1-R50"));
    }

    @Test
    public void replaceLongDashWithDoubleDash() {
        assertEquals("1--50", formatter.format("1 \u2014 50"));
    }

    @Test
    public void removePagePrefix() {
        assertEquals("50", formatter.format("p.50"));
    }

    @Test
    public void removePagesPrefix() {
        assertEquals("50", formatter.format("pp.50"));
    }

    @Test
    public void formatACMPages() {
        // This appears in https://doi.org/10.1145/1658373.1658375
        assertEquals("2:1--2:33", formatter.format("2:1-2:33"));
    }

    @Test
    public void keepFormattedACMPages() {
        // This appears in https://doi.org/10.1145/1658373.1658375
        assertEquals("2:1--2:33", formatter.format("2:1--2:33"));
    }

    @Test
    public void formatExample() {
        assertEquals("1--2", formatter.format(formatter.getExampleInput()));
    }

}
