package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringerQueryTransformerTest extends InfixTransformerTest<SpringerQueryTransformer> {

    @Override
    public String getAuthorPrefix() {
        return "name:";
    }

    @Override
    public SpringerQueryTransformer getTransformer() {
        return new SpringerQueryTransformer();
    }

    @Override
    public String getUnFieldedPrefix() {
        return "";
    }

    @Override
    public String getJournalPrefix() {
        return "journal:";
    }

    @Override
    public String getTitlePrefix() {
        return "title:";
    }

    @Override
    @Test
    public void convertYearField() throws ParseCancellationException {
        String queryString = "year=2015";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);

        Optional<String> expected = Optional.of("date:2015*");
        assertEquals(expected, query);
    }

    @Override
    @Test
    public void convertYearRangeField() throws ParseCancellationException {
        String queryString = "year-range=2012-2015";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);

        Optional<String> expected = Optional.of("date:2012* OR date:2013* OR date:2014* OR date:2015*");
        assertEquals(expected, query);
    }
}
