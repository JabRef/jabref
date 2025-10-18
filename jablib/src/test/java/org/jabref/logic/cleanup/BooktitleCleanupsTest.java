package org.jabref.logic.cleanup;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.JabRefException;
import org.jabref.logic.util.LocationDetector;
import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.BooktitleCleanupAction;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BooktitleCleanupsTest {
    private static LocationDetector testLocationDetector;

    @BeforeAll
    static void setUpLocationDetector() throws JabRefException {
        String testLocationData = """
                paris
                london
                new york
                san francisco
                hong kong
                st petersburg
                al-kharijah
                'abas abad
                los angeles
                berlin
                vienna
                prague
                tokyo
                seoul
                fort st. john
                """;

        testLocationDetector = LocationDetector.createTestInstance(
                new ByteArrayInputStream(testLocationData.getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void disabledCleanupReturnsEmpty() {
        BooktitleCleanups disabledCleanups = new BooktitleCleanups(false, testLocationDetector);
        BibEntry testEntry = new BibEntry()
                .withField(StandardField.BOOKTITLE, "Workshop January in London, pp. 45-67");

        List<FieldChange> result = disabledCleanups.cleanup(testEntry);
        assertTrue(result.isEmpty(), "Disabled cleanup should return no changes");
    }

    @ParameterizedTest(name = "Year cleanup: \"{0}\" -> cleaned up booktitle:\"{1}\", year:\"{2}\" (action: {3})")
    @MethodSource("yearCleanupTestData")
    void yearCleanup(String input, String expectedBooktitle, String expectedYear, BooktitleCleanupAction yearAction, boolean hasExistingYear) {
        BooktitleCleanups yearCleanups = new BooktitleCleanups(
                yearAction,
                BooktitleCleanupAction.SKIP,
                BooktitleCleanupAction.SKIP,
                BooktitleCleanupAction.SKIP,
                testLocationDetector
        );

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.BOOKTITLE, expectedBooktitle)
                .withField(StandardField.YEAR, expectedYear);
        BibEntry testEntry = new BibEntry().withField(StandardField.BOOKTITLE, input);
        if (hasExistingYear) {
            testEntry.withField(StandardField.YEAR, "1999"); // Existing year that should be preserved/replaced
        }

        yearCleanups.cleanup(testEntry);

        assertEquals(expectedEntry, testEntry);
    }

    static Stream<Arguments> yearCleanupTestData() {
        return Stream.of(
                // REPLACE_IF_EMPTY action - no existing year
                Arguments.of("Conference 2020", "Conference", "2020", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("Workshop 1999 Meeting", "Workshop Meeting", "1999", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("Meeting 2019 and 2021", "Meeting and", "2021", BooktitleCleanupAction.REPLACE_IF_EMPTY, false), // Latest year
                Arguments.of("Event 1998 2020 2018", "Event", "2020", BooktitleCleanupAction.REPLACE_IF_EMPTY, false), // Latest year

                // REPLACE_IF_EMPTY action - with existing year (should not replace)
                Arguments.of("Conference 2020", "Conference", "1999", BooktitleCleanupAction.REPLACE_IF_EMPTY, true),
                Arguments.of("Workshop 2021", "Workshop", "1999", BooktitleCleanupAction.REPLACE_IF_EMPTY, true),

                // REPLACE action - always replaces
                Arguments.of("Conference 2020", "Conference", "2020", BooktitleCleanupAction.REPLACE, false),
                Arguments.of("Workshop 2021", "Workshop", "2021", BooktitleCleanupAction.REPLACE, true),

                // REMOVE_ONLY action - never sets field
                Arguments.of("Conference 2020", "Conference", "", BooktitleCleanupAction.REMOVE_ONLY, false),
                Arguments.of("Workshop 2019", "Workshop", "1999", BooktitleCleanupAction.REMOVE_ONLY, true), // Preserves existing

                // SKIP action - no processing
                Arguments.of("Conference 2020", "Conference 2020", "", BooktitleCleanupAction.SKIP, false),
                Arguments.of("Workshop 2019", "Workshop 2019", "1999", BooktitleCleanupAction.SKIP, true),

                // No years present
                Arguments.of("Conference on AI", "Conference on AI", "", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("Workshop ML", "Workshop ML", "1999", BooktitleCleanupAction.REPLACE_IF_EMPTY, true)
        );
    }

    @ParameterizedTest(name = "Month cleanup: \"{0}\" -> booktitle:\"{1}\", month:\"{2}\" (action: {3})")
    @MethodSource("monthCleanupTestData")
    void monthCleanup(String input, String expectedBooktitle, String expectedMonth, BooktitleCleanupAction monthAction, boolean hasExistingMonth) {
        BooktitleCleanups monthCleanups = new BooktitleCleanups(
                BooktitleCleanupAction.SKIP,
                monthAction,
                BooktitleCleanupAction.SKIP,
                BooktitleCleanupAction.SKIP,
                testLocationDetector
        );

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.BOOKTITLE, expectedBooktitle)
                .withField(StandardField.MONTH, expectedMonth);
        BibEntry testEntry = new BibEntry().withField(StandardField.BOOKTITLE, input);
        if (hasExistingMonth) {
            testEntry.withField(StandardField.MONTH, "December"); // Existing month
        }

        monthCleanups.cleanup(testEntry);

        assertEquals(expectedEntry, testEntry);
    }

    static Stream<Arguments> monthCleanupTestData() {
        return Stream.of(
                // REPLACE_IF_EMPTY action - no existing month
                Arguments.of("Conference January", "Conference", "January", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("Workshop in February 2020", "Workshop in 2020", "February", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("March Meeting April", "Meeting", "March", BooktitleCleanupAction.REPLACE_IF_EMPTY, false), // First occurrence
                Arguments.of("Conference Dec", "Conference", "Dec", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("Workshop sep Meeting", "Workshop Meeting", "sep", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),

                // REPLACE_IF_EMPTY action - with existing month (should not replace)
                Arguments.of("Conference January", "Conference", "December", BooktitleCleanupAction.REPLACE_IF_EMPTY, true),

                // REPLACE action - always replaces
                Arguments.of("Conference January", "Conference", "January", BooktitleCleanupAction.REPLACE, false),
                Arguments.of("Workshop February", "Workshop", "February", BooktitleCleanupAction.REPLACE, true),

                // REMOVE_ONLY action - never sets field
                Arguments.of("Conference January", "Conference", "", BooktitleCleanupAction.REMOVE_ONLY, false),
                Arguments.of("Workshop February", "Workshop", "December", BooktitleCleanupAction.REMOVE_ONLY, true), // Preserves existing

                // SKIP action - no processing
                Arguments.of("Conference January", "Conference January", "", BooktitleCleanupAction.SKIP, false),
                Arguments.of("Workshop February", "Workshop February", "December", BooktitleCleanupAction.SKIP, true),

                // No months present
                Arguments.of("Conference on AI", "Conference on AI", "", BooktitleCleanupAction.REPLACE_IF_EMPTY, false)
        );
    }

    @ParameterizedTest(name = "Page range cleanup: \"{0}\" -> booktitle:\"{1}\", pages:\"{2}\" (action: {3})")
    @MethodSource("pageRangeCleanupTestData")
    void pageRangeCleanup(String input, String expectedBooktitle, String expectedPages, BooktitleCleanupAction pageAction, boolean hasExistingPages) {
        BooktitleCleanups pageCleanups = new BooktitleCleanups(
                BooktitleCleanupAction.SKIP,
                BooktitleCleanupAction.SKIP,
                pageAction,
                BooktitleCleanupAction.SKIP,
                testLocationDetector
        );

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.BOOKTITLE, expectedBooktitle)
                .withField(StandardField.PAGES, expectedPages);
        BibEntry testEntry = new BibEntry().withField(StandardField.BOOKTITLE, input);
        if (hasExistingPages) {
            testEntry.withField(StandardField.PAGES, "1-10"); // Existing pages
        }

        pageCleanups.cleanup(testEntry);

        assertEquals(expectedEntry, testEntry);
    }

    static Stream<Arguments> pageRangeCleanupTestData() {
        return Stream.of(
                // REPLACE_IF_EMPTY action - no existing pages
                Arguments.of("Conference pages 45-67", "Conference pages", "45-67", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("Workshop, pages 123--456", "Workshop, pages", "123--456", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("Meeting 100-200", "Meeting", "100-200", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("Conference 45 - 67 and 100-200", "Conference and", "45 - 67", BooktitleCleanupAction.REPLACE_IF_EMPTY, false), // First occurrence
                Arguments.of("Workshop pp 50  --  75", "Workshop pp", "50  --  75", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),

                // REPLACE_IF_EMPTY action - with existing pages (should not replace)
                Arguments.of("Conference pages 45-67", "Conference pages", "1-10", BooktitleCleanupAction.REPLACE_IF_EMPTY, true),

                // REPLACE action - always replaces
                Arguments.of("Conference pages 45-67", "Conference pages", "45-67", BooktitleCleanupAction.REPLACE, false),
                Arguments.of("Workshop 123-456", "Workshop", "123-456", BooktitleCleanupAction.REPLACE, true),

                // REMOVE_ONLY action - never sets field
                Arguments.of("Conference pages 45-67", "Conference pages", "", BooktitleCleanupAction.REMOVE_ONLY, false),
                Arguments.of("Workshop 123-456", "Workshop", "1-10", BooktitleCleanupAction.REMOVE_ONLY, true), // Preserves existing

                // SKIP action - no processing
                Arguments.of("Conference pages 45-67", "Conference pages 45-67", "", BooktitleCleanupAction.SKIP, false),
                Arguments.of("Workshop 123-456", "Workshop 123-456", "1-10", BooktitleCleanupAction.SKIP, true),

                // No page ranges present
                Arguments.of("Conference on AI", "Conference on AI", "", BooktitleCleanupAction.REPLACE_IF_EMPTY, false)
        );
    }

    @ParameterizedTest(name = "Location cleanup: \"{0}\" -> booktitle:\"{1}\", location:\"{2}\" (action: {3})")
    @MethodSource("locationCleanupTestData")
    void locationCleanup(String input, String expectedBooktitle, String expectedLocation, BooktitleCleanupAction locationAction, boolean hasExistingLocation) {
        BooktitleCleanups locationCleanups = new BooktitleCleanups(
                BooktitleCleanupAction.SKIP,
                BooktitleCleanupAction.SKIP,
                BooktitleCleanupAction.SKIP,
                locationAction,
                testLocationDetector
        );

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.BOOKTITLE, expectedBooktitle)
                .withField(StandardField.LOCATION, expectedLocation);
        BibEntry testEntry = new BibEntry().withField(StandardField.BOOKTITLE, input);
        if (hasExistingLocation) {
            testEntry.withField(StandardField.LOCATION, "Boston"); // Existing location
        }

        locationCleanups.cleanup(testEntry);

        assertEquals(expectedEntry, testEntry);
    }

    static Stream<Arguments> locationCleanupTestData() {
        return Stream.of(
                // REPLACE_IF_EMPTY action - no existing location
                Arguments.of("Conference in Paris", "Conference in", "Paris", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("San Francisco Meeting", "Meeting", "San Francisco", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("Conference Paris and London", "Conference and", "Paris, London", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),
                Arguments.of("Workshop in Berlin, Vienna", "Workshop in", "Vienna, Berlin", BooktitleCleanupAction.REPLACE_IF_EMPTY, false),

                // REPLACE_IF_EMPTY action - with existing location (should not replace)
                Arguments.of("Conference in Paris", "Conference in", "Boston", BooktitleCleanupAction.REPLACE_IF_EMPTY, true),

                // REPLACE action - always replaces
                Arguments.of("Conference in Paris", "Conference in", "Paris", BooktitleCleanupAction.REPLACE, false),
                Arguments.of("London Workshop", "Workshop", "London", BooktitleCleanupAction.REPLACE, true),

                // REMOVE_ONLY action - never sets field
                Arguments.of("Conference in Paris", "Conference in", "", BooktitleCleanupAction.REMOVE_ONLY, false),
                Arguments.of("London Workshop", "Workshop", "Boston", BooktitleCleanupAction.REMOVE_ONLY, true), // Preserves existing

                // SKIP action - no processing
                Arguments.of("Conference in Paris", "Conference in Paris", "", BooktitleCleanupAction.SKIP, false),
                Arguments.of("London Workshop", "London Workshop", "Boston", BooktitleCleanupAction.SKIP, true),

                // No locations present
                Arguments.of("Conference on AI", "Conference on AI", "", BooktitleCleanupAction.REPLACE_IF_EMPTY, false)
        );
    }

    @ParameterizedTest(name = "Combined cleanup: \"{0}\"")
    @MethodSource("combinedCleanupTestData")
    void combinedCleanup(String input, String expectedBooktitle, String expectedYear, String expectedMonth, String expectedPages, String expectedLocation) {
        // All actions set to REPLACE_IF_EMPTY for comprehensive cleanup
        BooktitleCleanups fullCleanups = new BooktitleCleanups(
                BooktitleCleanupAction.REPLACE_IF_EMPTY,
                BooktitleCleanupAction.REPLACE_IF_EMPTY,
                BooktitleCleanupAction.REPLACE_IF_EMPTY,
                BooktitleCleanupAction.REPLACE_IF_EMPTY,
                testLocationDetector
        );

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.BOOKTITLE, expectedBooktitle)
                .withField(StandardField.YEAR, expectedYear)
                .withField(StandardField.MONTH, expectedMonth)
                .withField(StandardField.PAGES, expectedPages)
                .withField(StandardField.LOCATION, expectedLocation);
        BibEntry testEntry = new BibEntry().withField(StandardField.BOOKTITLE, input);

        fullCleanups.cleanup(testEntry);

        assertEquals(expectedEntry, testEntry);
    }

    static Stream<Arguments> combinedCleanupTestData() {
        return Stream.of(
                Arguments.of(
                        "Proceedings of the International Conference on Machine Learning, Paris, France, July 2020, 45-67",
                        "Proceedings of the International Conference on Machine Learning, France",
                        "2020", "July", "45-67", "Paris"
                ),
                Arguments.of(
                        "Workshop on AI, London, January 2019, pages 123--456",
                        "Workshop on AI, pages",
                        "2019", "January", "123--456", "London"
                ),
                Arguments.of(
                        "Conference   on  Deep  Learning  New York  ( 2021 )   10 - 20",
                        "Conference on Deep Learning",
                        "2021", "", "10 - 20", "New York"
                ),
                Arguments.of(
                        "International Workshop Berlin Vienna Feb 2018 2020 100-200 300-400",
                        "International Workshop", // Multiple locations removed
                        "2020", "Feb", "100-200", "Vienna, Berlin" // Latest year, first month, first page range
                ),
                Arguments.of(
                        "Meeting Tokyo March April 1999 2001 50--75",
                        "Meeting",
                        "2001", "March", "50--75", "Tokyo"
                )
        );
    }

    @ParameterizedTest(name = "Journal field fallback: \"{0}\"")
    @MethodSource("journalFallbackTestData")
    void journalFieldFallback(String journalTitle, String expectedJournal, String expectedYear) {
        BooktitleCleanups cleanups = new BooktitleCleanups(
                BooktitleCleanupAction.REPLACE_IF_EMPTY,
                BooktitleCleanupAction.SKIP,
                BooktitleCleanupAction.SKIP,
                BooktitleCleanupAction.SKIP,
                testLocationDetector
        );

        // Don't set booktitle, only journal
        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, expectedJournal)
                .withField(StandardField.YEAR, expectedYear);
        BibEntry testEntry = new BibEntry().withField(StandardField.JOURNAL, journalTitle);

        cleanups.cleanup(testEntry);

        assertEquals(expectedEntry, testEntry);
    }

    static Stream<Arguments> journalFallbackTestData() {
        return Stream.of(
                Arguments.of("Journal of Machine Learning 2020", "Journal of Machine Learning", "2020"),
                Arguments.of("IEEE Transactions 2021", "IEEE Transactions", "2021"),
                Arguments.of("Simple Journal", "Simple Journal", "")
        );
    }

    @ParameterizedTest(name = "Field changes verification: \"{0}\"")
    @MethodSource("fieldChangeTestData")
    void fieldChangesGenerated(String originalBooktitle, List<FieldChange> expectedChanges) {
        BooktitleCleanups cleanups = new BooktitleCleanups(
                BooktitleCleanupAction.REPLACE_IF_EMPTY,
                BooktitleCleanupAction.REPLACE_IF_EMPTY,
                BooktitleCleanupAction.REPLACE_IF_EMPTY,
                BooktitleCleanupAction.REPLACE_IF_EMPTY,
                testLocationDetector
        );

        BibEntry testEntry = new BibEntry().withField(StandardField.BOOKTITLE, originalBooktitle);
        List<FieldChange> actualChanges = cleanups.cleanup(testEntry);

        assertEquals(expectedChanges.size(), actualChanges.size(), "Should generate expected number of field changes");

        for (FieldChange expectedChange : expectedChanges) {
            // Have to compare each field change manually since FieldChange.equals() checks for the same instance of BibEntry
            boolean found = actualChanges.stream()
                                         .anyMatch(actualChange -> matchesIgnoringEntry(expectedChange, actualChange));
            assertTrue(found, "Expected change not found: " + expectedChange);
        }
    }

    private static boolean matchesIgnoringEntry(FieldChange expectedChange, FieldChange actualChange) {
        return actualChange.getField().equals(expectedChange.getField()) &&
                actualChange.getOldValue().equals(expectedChange.getOldValue()) &&
                actualChange.getNewValue().equals(expectedChange.getNewValue());
    }

    static Stream<Arguments> fieldChangeTestData() {
        return Stream.of(
                Arguments.of(
                        "AI Conference, Paris, July 2020, 45-67",
                        List.of(
                                new FieldChange(new BibEntry(), StandardField.LOCATION, "", "Paris"),
                                new FieldChange(new BibEntry(), StandardField.YEAR, "", "2020"),
                                new FieldChange(new BibEntry(), StandardField.MONTH, "", "July"),
                                new FieldChange(new BibEntry(), StandardField.PAGES, "", "45-67"),
                                new FieldChange(new BibEntry(), StandardField.BOOKTITLE, "AI Conference, Paris, July 2020, 45-67", "AI Conference")
                        )
                ),
                Arguments.of(
                        "Simple Conference",
                        List.of(
                                new FieldChange(new BibEntry(), StandardField.BOOKTITLE, "Simple Conference", "Simple Conference")
                        )
                ),
                Arguments.of(
                        "Conference 2020",
                        List.of(
                                new FieldChange(new BibEntry(), StandardField.YEAR, "", "2020"),
                                new FieldChange(new BibEntry(), StandardField.BOOKTITLE, "Conference 2020", "Conference")
                        )
                ),
                Arguments.of(
                        "Conference January",
                        List.of(
                                new FieldChange(new BibEntry(), StandardField.MONTH, "", "January"),
                                new FieldChange(new BibEntry(), StandardField.BOOKTITLE, "Conference January", "Conference")
                        )
                ),
                Arguments.of(
                        "Conference 45-67",
                        List.of(
                                new FieldChange(new BibEntry(), StandardField.PAGES, "", "45-67"),
                                new FieldChange(new BibEntry(), StandardField.BOOKTITLE, "Conference 45-67", "Conference")
                        )
                )
        );
    }

    // ==================== Basic Cleanup Tests For Artifact Cleanup ====================

    @ParameterizedTest(name = "Basic cleanup: \"{0}\" -> \"{1}\"")
    @MethodSource("basicCleanupTestData")
    void basicBooktitleCleanup(String input, String expectedOutput) {
        // Test with all actions set to REMOVE_ONLY to focus on basic cleanup
        BooktitleCleanups removeOnlyCleanups = new BooktitleCleanups(
                BooktitleCleanupAction.REMOVE_ONLY,
                BooktitleCleanupAction.REMOVE_ONLY,
                BooktitleCleanupAction.REMOVE_ONLY,
                BooktitleCleanupAction.REMOVE_ONLY,
                testLocationDetector
        );

        BibEntry expectedEntry = new BibEntry().withField(StandardField.BOOKTITLE, expectedOutput);
        BibEntry testEntry = new BibEntry().withField(StandardField.BOOKTITLE, input);
        removeOnlyCleanups.cleanup(testEntry);

        assertEquals(expectedEntry, testEntry);
    }

    static Stream<Arguments> basicCleanupTestData() {
        return Stream.of(
                // Multiple spaces cleanup
                Arguments.of("Conference  on  AI", "Conference on AI"),
                Arguments.of("Workshop    Machine    Learning", "Workshop Machine Learning"),
                Arguments.of("Title   with   many    spaces", "Title with many spaces"),

                // Space before punctuation cleanup
                Arguments.of("Conference , Meeting", "Conference, Meeting"),
                Arguments.of("Workshop ; Session", "Workshop; Session"),
                Arguments.of("Conference ? Meeting", "Conference? Meeting"),
                Arguments.of("Workshop . Session", "Workshop. Session"),

                // Consecutive punctuation cleanup
                Arguments.of("Conference,, Meeting", "Conference, Meeting"),
                Arguments.of("Workshop;; Session", "Workshop; Session"),
                Arguments.of("Mixed .,; punctuation", "Mixed; punctuation"),
                Arguments.of("Conference... Meeting", "Conference. Meeting"),

                // Space before closing brackets/parens
                Arguments.of("Conference ( AI )", "Conference (AI)"),
                Arguments.of("Workshop [ ML ]", "Workshop [ML]"),
                Arguments.of("Meeting { title }", "Meeting {title}"),
                Arguments.of("Event ( machine learning )", "Event (machine learning)"),

                // Delimiter trimming
                Arguments.of("  Conference  ", "Conference"),
                Arguments.of(",,Workshop,,", "Workshop"),
                Arguments.of("--Meeting--", "Meeting"),
                Arguments.of("::Title::", "Title"),
                Arguments.of("..Conference..", "Conference"),
                Arguments.of("___Workshop___", "Workshop"),

                // Edge cases
                Arguments.of("", ""),
                Arguments.of("   ", ""),
                Arguments.of(",.,.,", ""),
                Arguments.of("Single", "Single"),
                Arguments.of("Already Clean Title", "Already Clean Title")
        );
    }
}
