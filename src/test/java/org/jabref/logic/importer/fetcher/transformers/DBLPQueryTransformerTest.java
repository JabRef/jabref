package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DBLPQueryTransformerTest extends InfixTransformerTest<DBLPQueryTransformer> {

    @Override
    public DBLPQueryTransformer getTransformer() {
        return new DBLPQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "";
    }

    @Override
    public String getJournalPrefix() {
        return "";
    }

    @Override
    public String getTitlePrefix() {
        return "";
    }

    @Override
    public void convertYearField() throws Exception {
        String queryString = "year:2015";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of("2015");
        assertEquals(expected, searchQuery);
    }

    @Override
    public void convertYearRangeField() throws Exception {
        String queryString = "year-range:2012-2015";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of("2012|2013|2014|2015");
        assertEquals(expected, searchQuery);
    }
}
