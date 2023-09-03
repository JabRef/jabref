package org.jabref.logic.importer.fetcher.transformers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.strings.StringUtil;

import kong.unirest.json.JSONObject;
import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
        assertEquals(Optional.of(2023), start);
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

    private static Stream<Arguments> getJSONWithYearVariations() throws FetcherException {
        String baseString = "title:Ericksen-Leslie page:1 pageSize:20 must_have_pdf:false sortBy:relevance";
        List<String> withYearAndYearRange = List.of(
                StringUtil.join(new String[]{baseString, "year:2020"}, " ", 0, 2),
                StringUtil.join(new String[]{baseString, "year-range:2019-2023"}, " ", 0, 2)
        );

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("queryString", "Ericksen-Leslie");
        expectedJson.put("page", 1);
        expectedJson.put("pageSize", 20);
        expectedJson.put("must_have_pdf", "false");
        expectedJson.put("sortBy", "relevance");

        List<JSONObject> actualJSONObjects = new ArrayList<>();
        withYearAndYearRange.forEach(requestStr -> {
            QueryNode luceneQuery = null;
            try {
                luceneQuery = new StandardSyntaxParser().parse(requestStr, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
            } catch (QueryNodeParseException e) {
                throw new RuntimeException(e);
            }
            CiteSeerQueryTransformer transformer = new CiteSeerQueryTransformer();
            transformer.transformLuceneQuery(luceneQuery);
            actualJSONObjects.add(transformer.getJSONPayload());
        });

        Iterator<JSONObject> jsonObjectIterator = actualJSONObjects.iterator();
        return Stream.of(
                Arguments.of(expectedJson, 2020, 2020, jsonObjectIterator.next()),
                Arguments.of(expectedJson, 2019, 2023, jsonObjectIterator.next())
        );
    }

    @ParameterizedTest
    @MethodSource("getJSONWithYearVariations")
    public void compareJSONRequestsWithYearVariations(JSONObject expected, Integer yearStart, Integer yearEnd, JSONObject actual) throws Exception {
        expected.put("yearStart", yearStart);
        expected.put("yearEnd", yearEnd);
        assertEquals(expected, actual);
        expected.remove("yearStart");
        expected.remove("yearEnd");
    }
}
