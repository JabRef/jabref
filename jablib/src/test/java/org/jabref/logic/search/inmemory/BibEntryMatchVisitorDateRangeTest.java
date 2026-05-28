package org.jabref.logic.search.inmemory;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibEntryMatchVisitorDateRangeTest {

    private static final Character KEYWORD_SEPARATOR = ',';

    private boolean matches(String expression, BibEntry entry) {
        SearchQuery query = new SearchQuery(expression);
        BibEntryMatchVisitor visitor = new BibEntryMatchVisitor(
                entry,
                query.getSearchFlags(),
                KEYWORD_SEPARATOR);

        return Boolean.TRUE.equals(visitor.visit(query.getStartContext(expression)));
    }

    @Test
    void matchesYearGreaterThanOrEqual() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.YEAR, "2005");

        assertTrue(matches("year >= 2001", entry));
    }

    @Test
    void doesNotMatchYearBelowLowerBound() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.YEAR, "1999");

        assertFalse(matches("year >= 2001", entry));
    }

    @Test
    void matchesYearInsideRange() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.YEAR, "2005");

        assertTrue(matches("year >= 2001 AND year <= 2009", entry));
    }

    @Test
    void doesNotMatchYearAboveRange() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.YEAR, "2010");

        assertFalse(matches("year >= 2001 AND year <= 2009", entry));
    }

    @Test
    void matchesBoundaryYear() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.YEAR, "2001");

        assertTrue(matches("year >= 2001", entry));
    }

    @Test
    void yearComparisonUsesDateFieldAsAlias() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.DATE, "2022-11-05");

        assertTrue(matches("year >= 2020", entry));
    }

    @Test
    void invalidYearComparisonDoesNotMatch() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.YEAR, "2005");

        assertFalse(matches("year >= abc", entry));
    }

    @Test
    void matchesFullDateInsideRange() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.DATE, "2005-06-10");

        assertTrue(matches("date >= 2001-05-17 AND date <= 2009-02-18", entry));
    }

    @Test
    void doesNotMatchDateBeforeRange() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.DATE, "2000-01-01");

        assertFalse(matches("date >= 2001-05-17 AND date <= 2009-02-18", entry));
    }

    @Test
    void doesNotMatchDateAfterRange() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.DATE, "2010-01-01");

        assertFalse(matches("date >= 2001-05-17 AND date <= 2009-02-18", entry));
    }

    @Test
    void matchesBoundaryDate() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.DATE, "2001-05-17");

        assertTrue(matches("date >= 2001-05-17", entry));
    }

    @Test
    void matchesYearOnlyDateQuery() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.DATE, "2022-11-05");

        assertTrue(matches("date >= 2022", entry));
    }

    @Test
    void matchesYearMonthDateQuery() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.DATE, "2022-11-05");

        assertTrue(matches("date >= 2022-10", entry));
    }

    @Test
    void matchesFullDateQuery() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.DATE, "2022-11-05");

        assertTrue(matches("date >= 2022-11-01", entry));
    }

    @Test
    void doesNotMatchInvertedYearRange() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.YEAR, "2005");

        assertFalse(matches("year >= 2009 AND year <= 2001", entry));
    }
}
