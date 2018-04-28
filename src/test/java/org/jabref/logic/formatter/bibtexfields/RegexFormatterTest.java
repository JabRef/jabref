package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class RegexFormatterTest {

    private RegexFormatter formatter;

    @Test
    void spacesReplacedCorrectly() {
        formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-all-spaces", formatter.format("replace all spaces"));
    }

    @Test
    void protectedSpacesNotReplacedInSingleProtectedBlock() {
        formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-spaces-{not these ones}", formatter.format("replace spaces {not these ones}"));
    }

    @Test
    void protectedSpacesNotReplacedInTwoProtectedBlocks() {
        formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-spaces-{not these ones}-{or these ones}-but-these-ones", formatter.format("replace spaces {not these ones} {or these ones} but these ones"));
    }

    @Test
    void escapedBracesAreNotReplaced() {
        formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-spaces-\\{-these-ones\\}-and-these-ones", formatter.format("replace spaces \\{ these ones\\} and these ones"));
    }

    @Test
    void escapedBracesAreNotReplacedInTwoCases() {
        formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-spaces-\\{-these-ones\\},-these-ones,-and-\\{-these-ones\\}", formatter.format("replace spaces \\{ these ones\\}, these ones, and \\{ these ones\\}"));
    }

    @Test
    void escapedBracesAreNotReplacedAndProtectionStillWorks() {
        formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-spaces-{not these ones},-these-ones,-and-\\{-these-ones\\}", formatter.format("replace spaces {not these ones}, these ones, and \\{ these ones\\}"));
    }

    @Test
    void formatExample() {
        formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("Please-replace-the-spaces", formatter.format(formatter.getExampleInput()));
    }
}
