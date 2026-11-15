package org.jabref.logic.importer.fetcher;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BibSonomyYearFilterTest {

    private static BaseQueryNode parse(String query) {
        SearchQuery searchQuery = new SearchQuery(query);
        return new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "learning                           | 1999 | true",
            "year=2020                          | 2020 | true",
            "year=2020                          | 2021 | false",
            "year-range=2010-2012               | 2011 | true",
            "year-range=2010-2012               | 2013 | false",
            "year-range=2012-2010               | 2011 | true",
            "learning year=2020                 | 2020 | true",
            "learning year=2020                 | 2021 | false",
            "year=2020 OR year=2022             | 2020 | true",
            "year=2020 OR year=2022             | 2021 | false",
            "year=2020 OR year=2022             | 2022 | true",
            "year=2020 OR title=foo             | 1999 | true",
            "NOT year=2020                      | 2020 | false",
            "NOT year=2020                      | 2021 | true",
            "learning AND NOT year=2020         | 2020 | false",
            "NOT (year=2020 OR year=2022)       | 2022 | false",
            "NOT (year=2020 OR year=2022)       | 2021 | true",
            "(year=2020 OR title=foo) year=2021 | 2020 | false",
            "(year=2020 OR title=foo) year=2021 | 2021 | true"
    })
    void acceptsEntriesAccordingToYearTerms(String query, String entryYear, boolean expected) {
        BibEntry entry = new BibEntry().withField(StandardField.YEAR, entryYear);

        assertEquals(expected, new BibSonomyYearFilter(parse(query)).accepts(entry));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "learning                     | true",
            "year=2020                    | true",
            "learning year=2020           | true",
            "learning OR author=smith     | true",
            "NOT author=smith             | true",
            "year=2020 OR year=2022       | false",
            "year=2020 OR title=foo       | false",
            "NOT year=2020                | false",
            "learning AND NOT year=2020   | false"
    })
    void allYearTermsAreRequiredOnlyWithoutOrAndNotAroundYears(String query, boolean expected) {
        assertEquals(expected, BibSonomyYearFilter.allYearTermsAreRequired(parse(query)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "year=2020            | false",
            "year-range=2010-2012 | false",
            "NOT year=2020        | true",
            "learning             | true"
    })
    void entryWithoutYearOnlyMatchesQueriesNotRequiringAYear(String query, boolean expected) {
        assertEquals(expected, new BibSonomyYearFilter(parse(query)).accepts(new BibEntry()));
    }

    @Test
    void nonNumericYearCountsAsUnknown() {
        BibEntry entry = new BibEntry().withField(StandardField.YEAR, "in press");

        assertFalse(new BibSonomyYearFilter(parse("year=2020")).accepts(entry));
    }
}
