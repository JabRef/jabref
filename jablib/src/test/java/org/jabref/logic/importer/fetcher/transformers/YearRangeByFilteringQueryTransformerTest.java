package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class YearRangeByFilteringQueryTransformerTest<T extends YearRangeByFilteringQueryTransformer> extends InfixTransformerTest<T> {

    @Override
    @Test
    public void convertYearRangeField() throws ParseCancellationException {
        YearRangeByFilteringQueryTransformer transformer = getTransformer();

        String queryString = "year-range=2018-2021";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = transformer.transformSearchQuery(searchQueryList);

        // The API does not support querying for a year range
        // The implementation of the fetcher filters the results manually

        assertEquals(Optional.empty(), query);

        // The implementation sets the start year and end year values according to the query
        assertEquals(Optional.of(2018), transformer.getStartYear());
        assertEquals(Optional.of(2021), transformer.getEndYear());
    }
}
