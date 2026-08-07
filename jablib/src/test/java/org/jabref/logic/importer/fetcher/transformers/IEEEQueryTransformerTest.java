package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IEEEQueryTransformerTest extends InfixTransformerTest<IEEEQueryTransformer> {

    @Override
    public IEEEQueryTransformer getTransformer() {
        return new IEEEQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "author:";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "";
    }

    @Override
    public String getJournalPrefix() {
        return "publication_title:";
    }

    @Override
    public String getTitlePrefix() {
        return "article_title:";
    }

    @Override
    @Test
    public void convertJournalFieldPrefix() throws ParseCancellationException {
        IEEEQueryTransformer transformer = getTransformer();

        String queryString = "journal=Nature";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        transformer.transformSearchQuery(searchQueryList);

        assertEquals(Optional.of("Nature"), transformer.getJournal());
    }

    @Override
    @Test
    public void convertYearField() throws ParseCancellationException {
        // IEEE does not support year range
        // Thus, a generic test does not work

        IEEEQueryTransformer transformer = getTransformer();

        String queryString = "year=2021";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        transformer.transformSearchQuery(searchQueryList);

        assertEquals(Optional.of(2021), transformer.getStartYear());
        assertEquals(Optional.of(2021), transformer.getEndYear());
    }

    @Override
    @Test
    public void convertYearRangeField() throws ParseCancellationException {
        IEEEQueryTransformer transformer = getTransformer();

        String queryString = "year-range=2018-2021";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        transformer.transformSearchQuery(searchQueryList);

        assertEquals(Optional.of(2018), transformer.getStartYear());
        assertEquals(Optional.of(2021), transformer.getEndYear());
    }

    private static Stream<Arguments> getTitleTestData() {
        return Stream.of(
                Arguments.of("Overcoming AND Open AND Source AND Project AND Entry AND Barriers AND Portal AND Newcomers", "Overcoming Open Source Project Entry Barriers with a Portal for Newcomers"),
                Arguments.of("Overcoming AND Open AND Source AND Project AND Entry AND Barriers", "Overcoming Open Source Project Entry Barriers")
        );
    }

    @ParameterizedTest
    @MethodSource("getTitleTestData")
    void stopWordRemoval(String expected, String queryString) throws ParseCancellationException {
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        assertEquals(Optional.ofNullable(expected), query);
    }
}
