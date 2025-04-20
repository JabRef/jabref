package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Test;

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
    @Test
    public void convertYearField() throws Exception {
        String queryString = "year:2015";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        DBLPQueryTransformer transformer = getTransformer();
        Optional<String> searchQuery = transformer.transformLuceneQuery(luceneQuery);
        assertEquals(Optional.empty(), searchQuery);
        assertEquals(Optional.of(2015), transformer.getStartYear());
        assertEquals(Optional.of(2015), transformer.getEndYear());
    }

    @Override
    @Test
    public void convertYearRangeField() throws Exception {
        String queryString = "year-range:2012-2015";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        DBLPQueryTransformer transformer = getTransformer();
        Optional<String> searchQuery = transformer.transformLuceneQuery(luceneQuery);
        assertEquals(Optional.empty(), searchQuery);
        assertEquals(Optional.of(2012), transformer.getStartYear());
        assertEquals(Optional.of(2015), transformer.getEndYear());
    }
}
