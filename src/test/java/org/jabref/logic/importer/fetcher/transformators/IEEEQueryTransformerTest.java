package org.jabref.logic.importer.fetcher.transformators;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IEEEQueryTransformerTest implements InfixTransformerTest {

    @Override
    public AbstractQueryTransformer getTransformator() {
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
        IEEEQueryTransformer transformer = ((IEEEQueryTransformer) getTransformator());

        String queryString = "journal:Nature";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals("\"Nature\"", transformer.getJournal().get());
    }

    @Override
    public void convertYearField() throws Exception {
        IEEEQueryTransformer transformer = ((IEEEQueryTransformer) getTransformator());

        String queryString = "year:2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals(2021, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }

    @Override
    public void convertYearRangeField() throws Exception {

        IEEEQueryTransformer transformer = ((IEEEQueryTransformer) getTransformator());

        String queryString = "year-range:2018-2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals(2018, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }
}
