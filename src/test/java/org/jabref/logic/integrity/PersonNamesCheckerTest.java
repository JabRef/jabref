package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PersonNamesCheckerTest {

    private PersonNamesChecker checker;
    private PersonNamesChecker checkerb;

    @BeforeEach
    public void setUp() throws Exception {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.setMode(BibDatabaseMode.BIBTEX);
        checker = new PersonNamesChecker(databaseContext);
        BibDatabaseContext database = new BibDatabaseContext();
        database.setMode(BibDatabaseMode.BIBLATEX);
        checkerb = new PersonNamesChecker(database);
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

    @ParameterizedTest
    @MethodSource("provideCorrectFormats")
    public void authorNameInCorrectFormatsShouldNotComplain(String input) {
        assertEquals(Optional.empty(), checkerb.checkValue(input));
    }

    @ParameterizedTest
    @MethodSource("provideIncorrectFormats")
    public void authorNameInIncorrectFormatsShouldComplain(String input) {
        assertNotEquals(Optional.empty(), checkerb.checkValue(input));
    }

    private static Stream<String> provideCorrectFormats() {
        return Stream.of("", "Knuth", "Donald E. Knuth and Kurt Cobain and A. Einstein");
    }

    private static Stream<String> provideIncorrectFormats() {
        return Stream.of("   Knuth, Donald E. ",
                         "Knuth, Donald E. and Kurt Cobain and A. Einstein",
                                      ", and Kurt Cobain and A. Einstein", "Donald E. Knuth and Kurt Cobain and ,",
                         "and Kurt Cobain and A. Einstein", "Donald E. Knuth and Kurt Cobain and");
    }

}
