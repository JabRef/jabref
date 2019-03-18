package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class RegexFormatterTest {

    private RegexFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RegexFormatter("(\" \",\"-\")");
    }

    @Test
    void spacesReplacedCorrectly() {
        //formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-all-spaces", formatter.format("replace all spaces"));
    }

    @Test
    void protectedSpacesNotReplacedInSingleProtectedBlock() {
        //formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-spaces-{not these ones}", formatter.format("replace spaces {not these ones}"));
    }

    @Test
    void protectedSpacesNotReplacedInTwoProtectedBlocks() {
        //formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-spaces-{not these ones}-{or these ones}-but-these-ones", formatter.format("replace spaces {not these ones} {or these ones} but these ones"));
    }

    @Test
    void escapedBracesAreNotReplaced() {
        //formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-spaces-\\{-these-ones\\}-and-these-ones", formatter.format("replace spaces \\{ these ones\\} and these ones"));
    }

    @Test
    void escapedBracesAreNotReplacedInTwoCases() {
        //formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-spaces-\\{-these-ones\\},-these-ones,-and-\\{-these-ones\\}", formatter.format("replace spaces \\{ these ones\\}, these ones, and \\{ these ones\\}"));
    }

    @Test
    void escapedBracesAreNotReplacedAndProtectionStillWorks() {
        //formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("replace-spaces-{not these ones},-these-ones,-and-\\{-these-ones\\}", formatter.format("replace spaces {not these ones}, these ones, and \\{ these ones\\}"));
    }

    @Test
    void formatExample() {
        //formatter = new RegexFormatter("(\" \",\"-\")");
        assertEquals("Please-replace-the-spaces", formatter.format(formatter.getExampleInput()));
    }

    @Test
    public void givenLocalizationLanguageSetToEnglish_whenGetNameMethod_thenRegularExpressionReturned() {
        Localization.setLanguage(Language.English);
        assertEquals("regular expression", formatter.getName());
    }

    @Test
    public void givenLocalizationLanguageSetToEnglish_whenGetDescriptionMethod_thenAddARegularMsgReturned() {
        Localization.setLanguage(Language.English);
        assertEquals("Add a regular expression for the key pattern.", formatter.getDescription());
    }

    @Test
    public void whenGetKeyMethod_thenRegexReturned() {
        assertEquals("regex", formatter.getKey());
    }

    //TODO: delete this commented test when confident the regex null check condition is completely unnecessary
    /*//trying to test 1 line of code but I don't think its possible to generate a null regex
    @Test
    public void givenNullRegex_whenFormatMethod_thenTheInputReturned(){
        String testInput = "test Input";
        String nullString = null;
        String regex = "(\" \",\"-\")";
        RegexFormatter sut = new RegexFormatter(regex);
        String returnedInput = sut.format(testInput);
        assertEquals(testInput, returnedInput);
    }*/

}
