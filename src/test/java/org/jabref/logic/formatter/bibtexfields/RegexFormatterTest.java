package org.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class RegexFormatterTest {

    private RegexFormatter formatter;

    @Before
    public void setUp() {
        formatter = new RegexFormatter();
    }

    @Test
    public void spacesReplacedCorrectly() {
        String regexInput = "(\" \",\"-\")";
        formatter.setRegex(regexInput);
        Assert.assertEquals("replace-all-spaces", formatter.format("replace all spaces"));
    }

    @Test
    public void protectedSpacesNotReplacedInSingleProtectedBlock() {
        String regexInput = "(\" \",\"-\")";
        formatter.setRegex(regexInput);
        Assert.assertEquals("replace-spaces-{not these ones}", formatter.format("replace spaces {not these ones}"));
    }

    @Test
    public void protectedSpacesNotReplacedInTwoProtectedBlocks() {
        String regexInput = "(\" \",\"-\")";
        formatter.setRegex(regexInput);
        Assert.assertEquals("replace-spaces-{not these ones}-{or these ones}-but-these-ones", formatter.format("replace spaces {not these ones} {or these ones} but these ones"));
    }

    @Test
    public void escapedBracesAreNotReplaced() {
        String regexInput = "(\" \",\"-\")";
        formatter.setRegex(regexInput);
        Assert.assertEquals("replace-spaces-\\{-these-ones\\}-and-these-ones", formatter.format("replace spaces \\{ these ones\\} and these ones"));
    }

    @Test
    public void escapedBracesAreNotReplacedInTwoCases() {
        String regexInput = "(\" \",\"-\")";
        formatter.setRegex(regexInput);
        Assert.assertEquals("replace-spaces-\\{-these-ones\\},-these-ones,-and-\\{-these-ones\\}", formatter.format("replace spaces \\{ these ones\\}, these ones, and \\{ these ones\\}"));
    }

    @Test
    public void escapedBracesAreNotReplacedAndProtectionStillWorks() {
        String regexInput = "(\" \",\"-\")";
        formatter.setRegex(regexInput);
        Assert.assertEquals("replace-spaces-{not these ones},-these-ones,-and-\\{-these-ones\\}", formatter.format("replace spaces {not these ones}, these ones, and \\{ these ones\\}"));
    }

    @Test
    public void formatExample() {
        Assert.assertEquals("Please-replace-the-spaces", formatter.format(formatter.getExampleInput()));
    }

}
