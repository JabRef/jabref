package org.jabref.logic.importer.fetcher.transformators;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArXivQueryTransformerTest implements InfixTransformerTest {

    @Override
    public AbstractQueryTransformer getTransformer() {
        return new ArXivQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "au:";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "all:";
    }

    @Override
    public String getJournalPrefix() {
        return "jr:";
    }

    @Override
    public String getTitlePrefix() {
        return "ti:";
    }

    @Override
    public void convertYearField() throws Exception {
        ArXivQueryTransformer transformer = ((ArXivQueryTransformer) getTransformer());
        String queryString = "2018";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> query = transformer.transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(queryString);
        assertEquals(expected, query);
        assertEquals(2018, transformer.getStartYear());
        assertEquals(2018, transformer.getEndYear());
    }

    @Override
    public void convertYearRangeField() throws Exception {
        ArXivQueryTransformer transformer = ((ArXivQueryTransformer) getTransformer());

        String queryString = "year-range:2018-2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> result = transformer.transformLuceneQuery(luceneQuery);

        // The API does not support querying for a year range
        // The implementation of the fetcher filters the results manually

        // The implementations returns an empty query
        assertEquals(Optional.of(""), result);

        // The implementation sets the start year and end year values according to the query
        assertEquals(2018, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }
}
