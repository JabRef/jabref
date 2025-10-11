package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionOfComputerScienceBibliographiesQueryTransformerTest extends InfixTransformerTest<CollectionOfComputerScienceBibliographiesQueryTransformer> {

    @Override
    public CollectionOfComputerScienceBibliographiesQueryTransformer getTransformer() {
        return new CollectionOfComputerScienceBibliographiesQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "au:";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "";
    }

    @Override
    public String getJournalPrefix() {
        return "";
    }

    @Override
    public String getTitlePrefix() {
        return "ti:";
    }

    @Override
    @Test
    public void convertYearField() throws ParseCancellationException {
        String queryString = "year=2018";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        assertEquals(Optional.of("year:2018"), query);
    }

    @Override
    @Test
    public void convertYearRangeField() throws ParseCancellationException {
        String queryString = "year-range=2018-2021";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        assertEquals(Optional.of("year:2018 OR year:2019 OR year:2020 OR year:2021"), query);
    }
}
