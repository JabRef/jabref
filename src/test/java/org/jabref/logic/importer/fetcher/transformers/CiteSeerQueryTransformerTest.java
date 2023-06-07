package org.jabref.logic.importer.fetcher.transformers;

import java.util.List;
import java.util.Optional;

import kong.unirest.json.JSONObject;
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

        Optional<Integer> start = Optional.of(transformer.getJSONPayload().getInt("yearStart"));
        Optional<Integer> end = Optional.of(transformer.getJSONPayload().getInt("yearEnd"));
        assertEquals(Optional.of(202), start);
        assertEquals(Optional.of(2023), end);
    }

    @Override
    public void convertYearRangeField() throws Exception {
        String queryString = "year-range:2019-2023";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformLuceneQuery(luceneQuery);

        Optional<Integer> start = Optional.of(transformer.getJSONPayload().getInt("yearStart"));
        Optional<Integer> end = Optional.of(transformer.getJSONPayload().getInt("yearEnd"));
        assertEquals(Optional.of(2019), start);
        assertEquals(Optional.of(2023), end);
    }

    @Test
    public void convertPageField() throws Exception {
        String queryString = "page:2";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformLuceneQuery(luceneQuery);

        Optional<Integer> page = Optional.of(transformer.getJSONPayload().getInt("page"));
        assertEquals(Optional.of(2), page);
    }

    @Test
    public void convertPageSizeField() throws Exception {
        String queryString = "pageSize:20";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformLuceneQuery(luceneQuery);

        Optional<Integer> pageSize = Optional.of(transformer.getJSONPayload().getInt("pageSize"));
        assertEquals(Optional.of(20), pageSize);
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

    @Test
    public void convertMultipleAuthors() throws Exception {
        String queryString = "author:\"Wang Wei\" author:\"Zhang Pingwen\" author:\"Zhang Zhifei\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformLuceneQuery(luceneQuery);

        List<String> authorsActual = transformer.getJSONPayload().getJSONArray("author").toList();
        List<String> authorsExpected = List.of("Wang Wei", "Zhang Pingwen", "Zhang Zhifei");
        assertEquals(authorsExpected, authorsActual);
    }

    @Test
    public void prepareFullRequest() throws Exception {
        String queryString = "title:Ericksen-Leslie page:1 pageSize:20 must_have_pdf:false sortBy:relevance year-range:2019-2023";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformLuceneQuery(luceneQuery);

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("queryString", "Ericksen-Leslie");
        expectedJson.put("page", 1);
        expectedJson.put("pageSize", 20);
        expectedJson.put("must_have_pdf", "false");
        expectedJson.put("sortBy", "relevance");
        expectedJson.put("yearStart", 2019);
        expectedJson.put("yearEnd", 2023);

        assertEquals(expectedJson, transformer.getJSONPayload());
    }
}
