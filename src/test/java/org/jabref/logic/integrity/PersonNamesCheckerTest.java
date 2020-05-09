package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PersonNamesCheckerTest {

    private PersonNamesChecker checker;

    @BeforeEach
    public void setUp() throws Exception {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.setMode(BibDatabaseMode.BIBTEX);
        checker = new PersonNamesChecker(databaseContext);
    }

    @Test
    public void validNameFirstnameAuthor() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("Kolb, Stefan"));
    }

    @Test
    public void validNameFirstnameAuthors() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("Kolb, Stefan and Harrer, Simon"));
    }

    @Test
    public void validFirstnameNameAuthor() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("Stefan Kolb"));
    }

    @Test
    public void validFirstnameNameAuthors() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("Stefan Kolb and Simon Harrer"));
    }

    @Test
    public void complainAboutPersonStringWithTwoManyCommas() throws Exception {
        assertEquals(Optional.of("Names are not in the standard BibTeX format."),
                checker.checkValue("Test1, Test2, Test3, Test4, Test5, Test6"));
    }

    @Test
    public void doNotComplainAboutSecondNameInFront() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("M. J. Gotay"));
    }

    @Test
    public void validCorporateNameInBrackets() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("{JabRef}"));
    }

    @Test
    public void validCorporateNameAndPerson() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("{JabRef} and Stefan Kolb"));
        assertEquals(Optional.empty(), checker.checkValue("{JabRef} and Kolb, Stefan"));
    }

    @Test
    void authorAcceptsVoidInput() {
        assertEquals(Optional.empty(), checker.checkValue(""));
    }

    @Test
    void authorAcceptsLastNameOnly() {
        assertEquals(Optional.empty(), checker.checkValue("Knuth"));
    }

    @Test
    void authorDoesNotAcceptSpacesBeforeFormat() {
        assertNotEquals(Optional.empty(), checker.checkValue("   Knuth, Donald E. "));
    }

    @Test
    void authorDoesNotAcceptDifferentFormats() {
        assertNotEquals(Optional.empty(), checker.checkValue("Knuth, Donald E. and Kurt Cobain and A. Einstein"));
    }

    @Test
    void authorAcceptsMultipleAuthors() {
        assertEquals(Optional.empty(), checker.checkValue("Donald E. Knuth and Kurt Cobain and A. Einstein"));
    }

    @Test
    void authorCanNotStartWithComma() {
        assertNotEquals(Optional.empty(), checker.checkValue(", and Kurt Cobain and A. Einstein"));
    }

    @Test
    void authorDoesNotAcceptCommaAsAuthor() {
        assertNotEquals(Optional.empty(), checker.checkValue("Donald E. Knuth and Kurt Cobain and ,"));

    }

    @Test
    void authorCanNotStartWithAnd() {
        assertNotEquals(Optional.empty(), checker.checkValue("and Kurt Cobain and A. Einstein"));
    }

    @Test
    void authorDoesNotAcceptUnfinishedSentence() {
        assertNotEquals(Optional.empty(), checker.checkValue("Donald E. Knuth and Kurt Cobain and"));
    }

}
