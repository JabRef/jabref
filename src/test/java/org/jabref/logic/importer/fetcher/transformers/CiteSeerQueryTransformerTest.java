package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CiteSeerQueryTransformerTest extends InfixTransformerTest<CiteSeerQueryTransformer> {

    @Override
    protected CiteSeerQueryTransformer getTransformer() {
        return new CiteSeerQueryTransformer();
    }

    @Override
    public void convertYearField() throws Exception {
        String queryString = "year:2023";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformLuceneQuery(luceneQuery);

        Optional<String> start = Optional.of(transformer.getJSONPayload().get("yearStart").toString());
        Optional<String> end = Optional.of(getTransformer().getJSONPayload().get("yearEnd").toString());
        assertEquals(Optional.of("2023"), start);
        assertEquals(Optional.of("2023"), end);
    }

    @Override
    public void convertYearRangeField() throws Exception {
        String queryString = "year-range:2019-2023";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformLuceneQuery(luceneQuery);

        Optional<String> start = Optional.of(transformer.getJSONPayload().get("yearStart").toString());
        Optional<String> end = Optional.of(getTransformer().getJSONPayload().get("yearEnd").toString());
        assertEquals(Optional.of("2019"), start);
        assertEquals(Optional.of("2023"), end);
    }

    @Test
    public void convertPageField() throws Exception {
        String queryString = "page:2";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformLuceneQuery(luceneQuery);

        Optional<String> page = Optional.of(transformer.getJSONPayload().get("page").toString());
        assertEquals(Optional.of("2"), page);
    }

    @Test
    public void convertPageSizeField() throws Exception {
        String queryString = "pageSize:20";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformLuceneQuery(luceneQuery);

        Optional<String> pageSize = Optional.of(transformer.getJSONPayload().get("pageSize").toString());
        assertEquals(Optional.of("20"), pageSize);
    }

    @Test
    public void convertSortByField() throws Exception {
        String queryString = "sortBy:relevance";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformLuceneQuery(luceneQuery);

        Optional<String> sortBy = Optional.of(transformer.getJSONPayload().get("sortBy").toString());
        assertEquals(Optional.of("relevance"), sortBy);
    }
}
