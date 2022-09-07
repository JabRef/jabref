package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PagesCheckerBibtexTest {

    private PagesChecker checker;

    @BeforeEach
    void setUp() {
        BibDatabaseContext database = new BibDatabaseContext();
        database.setMode(BibDatabaseMode.BIBTEX);
        checker = new PagesChecker(database);
    }

    public static Stream<String> bibtexAccepts() {
        return Stream.of("",
                // double dash
                "1--2",
                // one page number
                "12",
                // Multiple numbers
                "1,2,3",
                // bibTexAcceptsNoSimpleRangeOfNumbers
                "43+",
                // bibTexAcceptsMorePageNumbersWithRangeOfNumbers
                "7+,41--43,73"
                );
    }

    @ParameterizedTest
    @MethodSource
    void bibtexAccepts(String source) {
        assertEquals(Optional.empty(), checker.checkValue(source));
    }

    public static Stream<String> bibtexRejects() {
        return Stream.of(
                // hex numbers forbidden
                "777e23",
                // bibTexDoesNotAcceptRangeOfNumbersWithSingleDash
                "1-2",
                // bibTexDoesNotAcceptMorePageNumbersWithoutComma
                "1 2",
                // bibTexDoesNotAcceptBrackets
                "{1}-{2}"
                );
    }

    @ParameterizedTest
    @MethodSource
    void bibtexRejects(String source) {
        assertNotEquals(Optional.empty(), checker.checkValue(source));
    }
}
