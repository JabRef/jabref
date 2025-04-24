package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class YearAndYearRangeByFilteringQueryTransformerTest<T extends YearAndYearRangeByFilteringQueryTransformer> extends YearRangeByFilteringQueryTransformerTest<T> {
    @Override
    @Test
    public void convertYearField() throws Exception {
        YearAndYearRangeByFilteringQueryTransformer transformer = getTransformer();
        String queryString = "year:2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> query = transformer.transformLuceneQuery(luceneQuery);
        assertEquals(Optional.empty(), query);
        assertEquals(Optional.of(2021), transformer.getStartYear());
        assertEquals(Optional.of(2021), transformer.getEndYear());
    }
}
