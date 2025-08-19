package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class YearAndYearRangeByFilteringQueryTransformerTest<T extends YearAndYearRangeByFilteringQueryTransformer> extends YearRangeByFilteringQueryTransformerTest<T> {
    @Override
    @Test
    public void convertYearField() throws QueryNodeParseException {
        YearAndYearRangeByFilteringQueryTransformer transformer = getTransformer();
        String queryString = "year=2021";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        assertEquals(Optional.empty(), query);
        assertEquals(Optional.of(2021), transformer.getStartYear());
        assertEquals(Optional.of(2021), transformer.getEndYear());
    }
}
