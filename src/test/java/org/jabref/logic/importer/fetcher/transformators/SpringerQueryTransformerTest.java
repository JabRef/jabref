package org.jabref.logic.importer.fetcher.transformators;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpringerQueryTransformerTest implements InfixTransformerTest {

    @Override
    public String getAuthorPrefix() {
        return "name:";
    }

    @Override
    public AbstractQueryTransformer getTransformator() {
        return new SpringerQueryTransformer();
    }

    @Override
    public String getUnFieldedPrefix() {
        return "";
    }

    @Override
    public String getJournalPrefix() {
        return "journal:";
    }

    @Override
    public String getTitlePrefix() {
        return "title:";
    }

    @Test
    public void convertYearField() throws Exception {
        String searchQuery = getTransformator().parseQueryStringIntoComplexQuery("year:2015").get();
        assertEquals("date:2015*", searchQuery);
    }

    @Test
    public void convertNumericallyFirstYearField() throws Exception {
        String searchQuery = getTransformator().parseQueryStringIntoComplexQuery("year:2015 year:2014").get();
        assertEquals("date:2015* AND date:2014*", searchQuery);
    }

    @Test
    public void convertYearRangeField() throws Exception {
        String searchQuery = getTransformator().parseQueryStringIntoComplexQuery("year-range:2012-2015").get();
        assertEquals("date:2012* OR date:2013* OR date:2014* OR date:2015*", searchQuery);
    }
}
