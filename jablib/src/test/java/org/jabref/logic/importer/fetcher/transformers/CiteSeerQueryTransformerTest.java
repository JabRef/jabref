package org.jabref.logic.importer.fetcher.transformers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.strings.StringUtil;

import kong.unirest.core.json.JSONObject;
import org.antlr.v4.runtime.misc.ParseCancellationException;
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
    @Test
    public void convertYearField() throws ParseCancellationException {
        String queryString = "year=2023";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformSearchQuery(searchQueryList);

        Optional<Integer> start = Optional.of(transformer.getJSONPayload().getInt("yearStart"));
        Optional<Integer> end = Optional.of(transformer.getJSONPayload().getInt("yearEnd"));
        assertEquals(Optional.of(2023), start);
        assertEquals(Optional.of(2023), end);
    }

    @Override
    @Test
    public void convertYearRangeField() throws ParseCancellationException {
        String queryString = "year-range=2019-2023";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformSearchQuery(searchQueryList);

        Optional<Integer> start = Optional.of(transformer.getJSONPayload().getInt("yearStart"));
        Optional<Integer> end = Optional.of(transformer.getJSONPayload().getInt("yearEnd"));
        assertEquals(Optional.of(2019), start);
        assertEquals(Optional.of(2023), end);
    }

    @Test
    void convertPageField() throws ParseCancellationException {
        String queryString = "page=2";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformSearchQuery(searchQueryList);

        Optional<Integer> page = Optional.of(transformer.getJSONPayload().getInt("page"));
        assertEquals(Optional.of(2), page);
    }

    @Test
    void convertPageSizeField() throws ParseCancellationException {
        String queryString = "pageSize=20";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformSearchQuery(searchQueryList);

        Optional<Integer> pageSize = Optional.of(transformer.getJSONPayload().getInt("pageSize"));
        assertEquals(Optional.of(20), pageSize);
    }

    @Test
    void convertSortByField() throws ParseCancellationException {
        String queryString = "sortBy=relevance";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformSearchQuery(searchQueryList);

        Optional<String> sortBy = Optional.of(transformer.getJSONPayload().get("sortBy").toString());
        assertEquals(Optional.of("relevance"), sortBy);
    }

    @Test
    void convertMultipleAuthors() throws ParseCancellationException {
        String queryString = "author=\"Wang Wei\" author=\"Zhang Pingwen\" author=\"Zhang Zhifei\"";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        CiteSeerQueryTransformer transformer = getTransformer();
        transformer.transformSearchQuery(searchQueryList);

        List<String> authorsActual = transformer.getJSONPayload().getJSONArray("author").toList();
        List<String> authorsExpected = List.of("Wang Wei", "Zhang Pingwen", "Zhang Zhifei");
        assertEquals(authorsExpected, authorsActual);
    }

    private static Stream<Arguments> getJSONWithYearVariations() {
        String baseString = "title=Ericksen-Leslie page=1 pageSize=20 must_have_pdf=false sortBy=relevance";
        List<String> withYearAndYearRange = List.of(
                StringUtil.join(new String[] {baseString, "year=2020"}, " ", 0, 2),
                StringUtil.join(new String[] {baseString, "year-range=2019-2023"}, " ", 0, 2)
        );

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("queryString", "Ericksen-Leslie");
        expectedJson.put("page", 1);
        expectedJson.put("pageSize", 20);
        expectedJson.put("must_have_pdf", "false");
        expectedJson.put("sortBy", "relevance");

        List<JSONObject> actualJSONObjects = new ArrayList<>();
        withYearAndYearRange.forEach(requestStr -> {
            SearchQuery searchQuery = new SearchQuery(requestStr);
            BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
            CiteSeerQueryTransformer transformer = new CiteSeerQueryTransformer();
            transformer.transformSearchQuery(searchQueryList);
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
    void compareJSONRequestsWithYearVariations(JSONObject expected, Integer yearStart, Integer yearEnd, JSONObject actual) {
        expected.put("yearStart", yearStart);
        expected.put("yearEnd", yearEnd);
        assertEquals(expected, actual);
        expected.remove("yearStart");
        expected.remove("yearEnd");
    }
}
