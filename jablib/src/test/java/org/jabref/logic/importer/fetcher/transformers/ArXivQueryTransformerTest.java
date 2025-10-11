package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArXivQueryTransformerTest extends YearRangeByFilteringQueryTransformerTest<ArXivQueryTransformer> {

    @Override
    public ArXivQueryTransformer getTransformer() {
        return new ArXivQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "au:";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "all:";
    }

    @Override
    public String getJournalPrefix() {
        return "jr:";
    }

    @Override
    public String getTitlePrefix() {
        return "ti:";
    }

    @Override
    @Test
    public void convertYearField() throws ParseCancellationException {
        ArXivQueryTransformer transformer = getTransformer();
        String queryString = "year=2018";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = transformer.transformSearchQuery(searchQueryList);
        assertEquals(Optional.of("2018"), query);
        assertEquals(Optional.of(2018), transformer.getStartYear());
        assertEquals(Optional.of(2018), transformer.getEndYear());
    }
}
