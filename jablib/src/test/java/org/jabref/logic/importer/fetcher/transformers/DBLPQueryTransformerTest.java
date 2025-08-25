package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DBLPQueryTransformerTest extends InfixTransformerTest<DBLPQueryTransformer> {

    @Override
    public DBLPQueryTransformer getTransformer() {
        return new DBLPQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "";
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
        return "";
    }

    @Override
    @Test
    public void convertYearField() throws ParseCancellationException {
        String queryString = "year=2015";
        DBLPQueryTransformer transformer = getTransformer();
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = transformer.transformSearchQuery(searchQueryList);
        assertEquals(Optional.empty(), query);
        assertEquals(Optional.of(2015), transformer.getStartYear());
        assertEquals(Optional.of(2015), transformer.getEndYear());
    }

    @Override
    @Test
    public void convertYearRangeField() throws ParseCancellationException {
        String queryString = "year-range=2012-2015";
        DBLPQueryTransformer transformer = getTransformer();
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = transformer.transformSearchQuery(searchQueryList);
        assertEquals(Optional.empty(), query);
        assertEquals(Optional.of(2012), transformer.getStartYear());
        assertEquals(Optional.of(2015), transformer.getEndYear());
    }
}
