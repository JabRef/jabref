package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ResourceLock("Localization.lang")
public class PagesCheckerBiblatexTest {

    private PagesChecker checker;

    @BeforeEach
    void setUp() {
        BibDatabaseContext database = new BibDatabaseContext();
        database.setMode(BibDatabaseMode.BIBLATEX);
        checker = new PagesChecker(database);
    }

    public static Stream<String> bibtexAccepts() {
        return Stream.of("",
                // single dash
                "1-2",
                // double dash
                "1--2",
                // one page number
                "12",
                // Multiple numbers
                "1,2,3",
                // bibTexAcceptsNoSimpleRangeOfNumbers
                "43+",
                // bibTexAcceptsMorePageNumbersWithRangeOfNumbers
                "7+,41--43,73",
                // suffix
                "436S-439S",
                // prefix
                "S436-S439",
                // prefix and suffix
                "S436S-S439S",
                // affix and more following range
                "S10A+",
                // unicode separator
                "1\u201310",
                // other separators
                "A10S/90", "1/10",
                // sequens and sequents
                "1f", "1ff", "1 f.", "1 ff.", "1sq", "1sqq", "1 sq.", "1 sqq.",
                // roman numerals
                "i", "ivxlcdm", "IVXLCDM", "iS", "i-vi", "VII-xii"
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
                // bibTexDoesNotAcceptMorePageNumbersWithoutComma
                "1 2",
                // bibTexDoesNotAcceptBrackets
                "{1}-{2}",
                // More than one suffix forbidden
                "436SS-439S",
                // invalid ranges
                "10-", "-10", "10--", "--10", "+10", "10+-10"
        );
    }

    @ParameterizedTest
    @MethodSource
    void bibtexRejects(String source) {
        assertNotEquals(Optional.empty(), checker.checkValue(source));
    }
}
