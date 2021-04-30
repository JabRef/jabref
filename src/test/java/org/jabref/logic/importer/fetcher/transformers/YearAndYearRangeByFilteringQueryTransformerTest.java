package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class YearAndYearRangeByFilteringQueryTransformerTest<T extends YearAndYearRangeByFilteringQueryTransformer> extends YearRangeByFilteringQueryTransformerTest<T> {
    @Override
    public void convertYearField() throws Exception {
        YearAndYearRangeByFilteringQueryTransformer transformer = getTransformer();
        String queryString = "year:2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> query = transformer.transformLuceneQuery(luceneQuery);
        assertEquals(Optional.of(""), query);
        assertEquals(2021, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }
}
