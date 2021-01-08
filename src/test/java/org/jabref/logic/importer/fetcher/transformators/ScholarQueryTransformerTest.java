package org.jabref.logic.importer.fetcher.transformators;

import static org.junit.jupiter.api.Assertions.*;

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

        transformer.parseQueryStringIntoComplexQuery("year:2021");

        assertEquals(2021, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }

    @Override
    public void convertYearRangeField() throws Exception {

        ScholarQueryTransformer transformer = ((ScholarQueryTransformer) getTransformator());

        transformer.parseQueryStringIntoComplexQuery("year-range:2018-2021");

        assertEquals(2018, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }
}
