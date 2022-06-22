package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Defines the set of capability tests that each tests a given search capability, e.g. author based search.
 * The idea is to code the capabilities of a fetcher into Java code.
 * This way, a) the capabilities of a fetcher are checked automatically (because they can change from time-to-time by the provider)
 * and b) the queries sent to the fetchers can be debugged directly without a route through to some fetcher code.
 */
interface SearchBasedFetcherCapabilityTest {

    /**
     * Test whether the library API supports author field search.
     */
    @Test
    default void supportsAuthorSearch() throws Exception {
        StringJoiner queryBuilder = new StringJoiner("\" AND author:\"", "author:\"", "\"");
        getTestAuthors().forEach(queryBuilder::add);

        List<BibEntry> result = getFetcher().performSearch(queryBuilder.toString());
        new ImportCleanup(BibDatabaseMode.BIBTEX).doPostCleanup(result);

        assertFalse(result.isEmpty());
        result.forEach(bibEntry -> {
            String author = bibEntry.getField(StandardField.AUTHOR).orElse("");

            // The co-authors differ, thus we check for the author present at all papers
            getTestAuthors().forEach(expectedAuthor -> Assertions.assertTrue(author.contains(expectedAuthor.replace("\"", ""))));
        });
    }

    /**
     * Test whether the library API supports year field search.
     */
    @Test
    default void supportsYearSearch() throws Exception {
        List<BibEntry> result = getFetcher().performSearch("year:" + getTestYear());
        new ImportCleanup(BibDatabaseMode.BIBTEX).doPostCleanup(result);
        List<String> differentYearsInResult = result.stream()
                                                    .map(bibEntry -> bibEntry.getField(StandardField.YEAR))
                                                    .filter(Optional::isPresent)
                                                    .map(Optional::get)
                                                    .distinct()
                                                    .collect(Collectors.toList());

        assertEquals(Collections.singletonList(getTestYear().toString()), differentYearsInResult);
    }

    /**
     * Test whether the library API supports year range search.
     */
    @Test
    default void supportsYearRangeSearch() throws Exception {
        List<String> yearsInYearRange = List.of("2018", "2019", "2020");

        List<BibEntry> result = getFetcher().performSearch("year-range:2018-2020");
        new ImportCleanup(BibDatabaseMode.BIBTEX).doPostCleanup(result);
        List<String> differentYearsInResult = result.stream()
                                                    .map(bibEntry -> bibEntry.getField(StandardField.YEAR))
                                                    .filter(Optional::isPresent)
                                                    .map(Optional::get)
                                                    .distinct()
                                                    .collect(Collectors.toList());
        assertFalse(result.isEmpty());
        assertTrue(yearsInYearRange.containsAll(differentYearsInResult));
    }

    /**
     * Test whether the library API supports journal based search.
     */
    @Test
    default void supportsJournalSearch() throws Exception {
        List<BibEntry> result = getFetcher().performSearch("journal:\"" + getTestJournal() + "\"");
        new ImportCleanup(BibDatabaseMode.BIBTEX).doPostCleanup(result);

        assertFalse(result.isEmpty());
        result.forEach(bibEntry -> {
            assertTrue(bibEntry.hasField(StandardField.JOURNAL));
            String journal = bibEntry.getField(StandardField.JOURNAL).orElse("");
            assertTrue(journal.contains(getTestJournal().replace("\"", "")));
        });
    }

    SearchBasedFetcher getFetcher();

    List<String> getTestAuthors();

    String getTestJournal();

    default Integer getTestYear() {
        return 2016;
    }
}
