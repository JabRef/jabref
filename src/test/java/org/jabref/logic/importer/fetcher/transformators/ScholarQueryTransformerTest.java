package org.jabref.logic.importer.fetcher.transformators;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScholarQueryTransformerTest implements InfixTransformerTest {

    @Override
    public AbstractQueryTransformer getTransformator() {
        return new ScholarQueryTransformer();
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
        return "source:";
    }

    @Override
    public String getTitlePrefix() {
        return "allintitle:";
    }

    @Override
    public void convertYearField() throws Exception {
        ScholarQueryTransformer transformer = ((ScholarQueryTransformer) getTransformator());

        String queryString = "year:2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals(2021, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }

    @Override
    public void convertYearRangeField() throws Exception {

        ScholarQueryTransformer transformer = ((ScholarQueryTransformer) getTransformator());

        String queryString = "year-range:2018-2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals(2018, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }
}
