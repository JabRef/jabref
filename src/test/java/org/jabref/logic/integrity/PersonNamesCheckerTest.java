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

    @ParameterizedTest
    @MethodSource("provideValidNames")
    public void validNames(String name) {
        assertEquals(Optional.empty(), checker.checkValue(name));
    }

    private static Stream<String> provideValidNames() {
        return Stream.of(
                "Kolb, Stefan",                 // single [Name, Firstname]
                "Kolb, Stefan and Harrer, Simon",   // multiple [Name, Firstname]
                "Stefan Kolb",                      // single [Firstname Name]
                "Stefan Kolb and Simon Harrer",     // multiple [Firstname Name]

                "M. J. Gotay",                      // second name in front

                "{JabRef}",                         // corporate name in brackets
                "{JabRef} and Stefan Kolb",         // mixed corporate name with name
                "{JabRef} and Kolb, Stefan",

                "hugo Para{\\~n}os"                 // tilde in name
        );
    }

    @Test
    public void complainAboutPersonStringWithTwoManyCommas() {
        assertEquals(Optional.of("Names are not in the standard BibTeX format."),
                checker.checkValue("Test1, Test2, Test3, Test4, Test5, Test6"));
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
        return Stream.of(
                "",
                "Knuth",
                "Donald E. Knuth and Kurt Cobain and A. Einstein");
    }

    private static Stream<String> provideIncorrectFormats() {
        return Stream.of(
                "   Knuth, Donald E. ",
                "Knuth, Donald E. and Kurt Cobain and A. Einstein",
                ", and Kurt Cobain and A. Einstein",
                "Donald E. Knuth and Kurt Cobain and ,",
                "and Kurt Cobain and A. Einstein",
                "Donald E. Knuth and Kurt Cobain and");
    }
}
