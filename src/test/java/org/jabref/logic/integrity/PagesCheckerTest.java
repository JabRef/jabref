package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PagesCheckerTest {

    private PagesChecker checker;

    @BeforeEach
    void setUp() {
        BibDatabaseContext database = new BibDatabaseContext();
        database.setMode(BibDatabaseMode.BIBTEX);
        checker = new PagesChecker(database);
    }

    @Test
    void bibTexAcceptsRangeOfNumbersWithDoubleDash() {
        assertEquals(Optional.empty(), checker.checkValue("1--2"));
    }

    @Test
    void bibTexAcceptsOnePageNumber() {
        assertEquals(Optional.empty(), checker.checkValue("12"));
    }

    @Test
    void bibTexDoesNotAcceptRangeOfNumbersWithSingleDash() {
        assertNotEquals(Optional.empty(), checker.checkValue("1-2"));
    }

    @Test
    void bibTexAcceptsMorePageNumbers() {
        assertEquals(Optional.empty(), checker.checkValue("1,2,3"));
    }

    @Test
    void bibTexAcceptsNoSimpleRangeOfNumbers() {
        assertEquals(Optional.empty(), checker.checkValue("43+"));
    }

    @Test
    void bibTexDoesNotAcceptMorePageNumbersWithoutComma() {
        assertNotEquals(Optional.empty(), checker.checkValue("1 2"));
    }

    @Test
    void bibTexDoesNotAcceptBrackets() {
        assertNotEquals(Optional.empty(), checker.checkValue("{1}-{2}"));
    }

    @Test
    void bibTexAcceptsMorePageNumbersWithRangeOfNumbers() {
        assertEquals(Optional.empty(), checker.checkValue("7+,41--43,73"));
    }

}
