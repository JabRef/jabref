package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GVKQueryTransformerTest extends InfixTransformerTest<GVKQueryTransformer> {

    @Override
    public GVKQueryTransformer getTransformer() {
        return new GVKQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "pica.per=";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "pica.all=";
    }

    @Override
    public String getJournalPrefix() {
        return "pica.zti=";
    }

    @Override
    public String getTitlePrefix() {
        return "pica.tit=";
    }

    @Override
    @Test
    public void convertYearField() throws ParseCancellationException {
        String queryString = "year=2018";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);

        Optional<String> expected = Optional.of("pica.erj=2018");
        assertEquals(expected, query);
    }

    @Disabled("Not supported by GVK")
    @Override
    @Test
    public void convertYearRangeField() {
    }
}
