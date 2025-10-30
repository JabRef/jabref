package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZbMathQueryTransformerTest extends InfixTransformerTest<ZbMathQueryTransformer> {

    @Override
    public ZbMathQueryTransformer getTransformer() {
        return new ZbMathQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "au:";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "any:";
    }

    @Override
    public String getJournalPrefix() {
        return "so:";
    }

    @Override
    public String getTitlePrefix() {
        return "ti:";
    }

    @Override
    @Test
    public void convertYearField() throws ParseCancellationException {
        String queryString = "year=2015";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of("py:2015");
        assertEquals(expected, query);
    }

    @Override
    @Test
    public void convertYearRangeField() throws ParseCancellationException {
        String queryString = "year-range=2012-2015";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of("py:2012-2015");
        assertEquals(expected, query);
    }
}
