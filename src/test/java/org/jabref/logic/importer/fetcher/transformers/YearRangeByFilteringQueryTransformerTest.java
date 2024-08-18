package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class YearRangeByFilteringQueryTransformerTest<T extends YearRangeByFilteringQueryTransformer> extends InfixTransformerTest<T> {

    @Override
    @Test
    public void convertYearRangeField() throws Exception {
        YearRangeByFilteringQueryTransformer transformer = getTransformer();

        String queryString = "year-range:2018-2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> result = transformer.transformLuceneQuery(luceneQuery);

        // The API does not support querying for a year range
        // The implementation of the fetcher filters the results manually

        assertEquals(Optional.empty(), result);

        // The implementation sets the start year and end year values according to the query
        assertEquals(Optional.of(2018), transformer.getStartYear());
        assertEquals(Optional.of(2021), transformer.getEndYear());
    }
}
