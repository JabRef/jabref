package org.jabref.logic.importer.fetcher.transformators;

import static org.junit.jupiter.api.Assertions.*;

class IEEEQueryTransformerTest implements InfixTransformerTest{

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

        transformer.parseQueryStringIntoComplexQuery("journal:Nature");

        assertEquals("\"Nautre\"", transformer.getJournal().get());
    }

    @Override
    public void convertYearField() throws Exception {
        IEEEQueryTransformer transformer = ((IEEEQueryTransformer) getTransformator());

        transformer.parseQueryStringIntoComplexQuery("year:2021");

        assertEquals(2021, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }

    @Override
    public void convertYearRangeField() throws Exception {

        IEEEQueryTransformer transformer = ((IEEEQueryTransformer) getTransformator());

        transformer.parseQueryStringIntoComplexQuery("year-range:2018-2021");

        assertEquals(2018, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }
}
