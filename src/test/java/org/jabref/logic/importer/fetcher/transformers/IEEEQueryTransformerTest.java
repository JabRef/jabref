package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;

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
        IEEEQueryTransformer transformer = ((IEEEQueryTransformer) getTransformer());

        String queryString = "journal:Nature";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals("\"Nature\"", transformer.getJournal().get());
    }

    @Override
    public void convertYearField() throws Exception {
        // IEEE does not support year range
        // Thus, a generic test does not work

        IEEEQueryTransformer transformer = ((IEEEQueryTransformer) getTransformer());

        String queryString = "year:2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> result = transformer.transformLuceneQuery(luceneQuery);

        assertEquals(2021, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }

    @Override
    public void convertYearRangeField() throws Exception {
        IEEEQueryTransformer transformer = ((IEEEQueryTransformer) getTransformer());

        String queryString = "year-range:2018-2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals(2018, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }
}
