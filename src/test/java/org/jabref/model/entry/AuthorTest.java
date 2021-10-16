package org.jabref.model.entry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorTest {

    @Test
    void addDotIfAbbreviationAddDot() {
        assertEquals("O.", Author.addDotIfAbbreviation("O"));
        assertEquals("A. O.", Author.addDotIfAbbreviation("AO"));
        assertEquals("A. O.", Author.addDotIfAbbreviation("AO."));
        assertEquals("A. O.", Author.addDotIfAbbreviation("A.O."));
        assertEquals("A.-O.", Author.addDotIfAbbreviation("A-O"));
    }

    @Test
    void addDotIfAbbreviationDoesNotAddMultipleSpaces() {
        assertEquals("A. O.", Author.addDotIfAbbreviation("A O"));
    }

    @Test
    void addDotIfAbbreviationDoNotAddDot() {
        assertEquals("O.", Author.addDotIfAbbreviation("O."));
        assertEquals("A. O.", Author.addDotIfAbbreviation("A. O."));
        assertEquals("A.-O.", Author.addDotIfAbbreviation("A.-O."));
        assertEquals("O. Moore", Author.addDotIfAbbreviation("O. Moore"));
        assertEquals("A. O. Moore", Author.addDotIfAbbreviation("A. O. Moore"));
        assertEquals("O. von Moore", Author.addDotIfAbbreviation("O. von Moore"));
        assertEquals("A.-O. Moore", Author.addDotIfAbbreviation("A.-O. Moore"));
        assertEquals("Moore, O.", Author.addDotIfAbbreviation("Moore, O."));
        assertEquals("Moore, O., Jr.", Author.addDotIfAbbreviation("Moore, O., Jr."));
        assertEquals("Moore, A. O.", Author.addDotIfAbbreviation("Moore, A. O."));
        assertEquals("Moore, A.-O.", Author.addDotIfAbbreviation("Moore, A.-O."));
        assertEquals("MEmre", Author.addDotIfAbbreviation("MEmre"));
        assertEquals("{\\'{E}}douard", Author.addDotIfAbbreviation("{\\'{E}}douard"));
        assertEquals("J{\\\"o}rg", Author.addDotIfAbbreviation("J{\\\"o}rg"));
        assertEquals("Moore, O. and O. Moore", Author.addDotIfAbbreviation("Moore, O. and O. Moore"));
        assertEquals("Moore, O. and O. Moore and Moore, O. O.", Author.addDotIfAbbreviation("Moore, O. and O. Moore and Moore, O. O."));
    }

    @Test
    void addDotIfAbbreviationIfNameIsNull() {
        String nameTest = null;
        assertEquals(null, Author.addDotIfAbbreviation(nameTest));
    }

    @Test
    void addDotIfAbbreviationReturnNameIfEmptyString() {
        assertEquals("", Author.addDotIfAbbreviation(""));
    }

    @Test
    void addDotIfAbbreviationMoreLowerCaseLetters() {
        assertEquals("asdf", Author.addDotIfAbbreviation("asdf"));
    }

    @Test
    void addDotIfAbbreviationOnlyOneCharLowerCase() {
        assertEquals("a", Author.addDotIfAbbreviation("a"));
    }

    @Test
    void addDotIfAbbreviationStartWithUpperCaseAndHyphen() {
        assertEquals("A.-melia", Author.addDotIfAbbreviation("A-melia"));
    }

    @Test
    void addDotIfAbbreviationEndsWithUpperCaseLetter() {
        assertEquals("AmeliA", Author.addDotIfAbbreviation("AmeliA"));
    }

    @Test
    void addDotIfAbbreviationEndsWithUpperCaseLetterSpaced() {
        assertEquals("Ameli A.", Author.addDotIfAbbreviation("Ameli A"));
    }

    @Test
    void addDotIfAbbreviationEndsWithWhiteSpaced() {
        assertEquals("Ameli", Author.addDotIfAbbreviation("Ameli "));
    }

    @Test
    void addDotIfAbbreviationEndsWithDoubleAbbreviation() {
        assertEquals("Ameli A. A.", Author.addDotIfAbbreviation("Ameli AA"));
    }

    @Test
    void addDotIfAbbreviationIfStartsWithNumber() {
        assertEquals("1", Author.addDotIfAbbreviation("1"));
    }

    @Test
    void addDotIfAbbreviationIfStartsWithSpacedNumber() {
        assertEquals("1 23", Author.addDotIfAbbreviation("1 23"));
    }

}
