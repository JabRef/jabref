package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IEEEQueryTransformerTest extends InfixTransformerTest<IEEEQueryTransformer> {

    @Override
    public IEEEQueryTransformer getTransformer() {
        return new IEEEQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "author:";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "";
    }

    @Override
    public String getJournalPrefix() {
        return "publication_title:";
    }

    @Override
    public String getTitlePrefix() {
        return "article_title:";
    }

    @Override
    public void convertJournalField() throws Exception {
        IEEEQueryTransformer transformer = getTransformer();

        String queryString = "journal:Nature";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals("\"Nature\"", transformer.getJournal().get());
    }

    @Override
    public void convertYearField() throws Exception {
        // IEEE does not support year range
        // Thus, a generic test does not work

        IEEEQueryTransformer transformer = getTransformer();

        String queryString = "year:2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals(2021, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }

    @Override
    public void convertYearRangeField() throws Exception {
        IEEEQueryTransformer transformer = getTransformer();

        String queryString = "year-range:2018-2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals(2018, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }

    private static Stream<Arguments> getTitleTestData() {
        return Stream.of(
                Arguments.of("Overcoming AND Open AND Source AND Project AND Entry AND Barriers AND Portal AND Newcomers", "Overcoming Open Source Project Entry Barriers with a Portal for Newcomers"),
                Arguments.of("Overcoming AND Open AND Source AND Project AND Entry AND Barriers", "Overcoming Open Source Project Entry Barriers"),
                Arguments.of(null, "and")
        );
    }

    @ParameterizedTest
    @MethodSource("getTitleTestData")
    public void testStopWordRemoval(String expected, String queryString) throws Exception {
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> result = getTransformer().transformLuceneQuery(luceneQuery);
        assertEquals(Optional.ofNullable(expected), result);
    }
}
