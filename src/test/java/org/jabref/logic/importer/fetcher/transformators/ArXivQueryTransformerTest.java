package org.jabref.logic.importer.fetcher.transformators;

import java.util.Optional;

import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.*;

class ArXivQueryTransformerTest implements InfixTransformerTest {

    @Override
    public AbstractQueryTransformer getTransformator() {
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
        ArXivQueryTransformer transformer = ((ArXivQueryTransformer) getTransformator());
        Optional<String> query = transformer.parseQueryStringIntoComplexQuery("year:2018");
        Optional<String> expected = Optional.of("2018");
        assertEquals(expected, query);
        assertEquals(2018, transformer.getStartYear());
        assertEquals(2018, transformer.getEndYear());
    }

    @Override
    public void convertYearRangeField() throws Exception {
        ArXivQueryTransformer transformer = ((ArXivQueryTransformer) getTransformator());

        transformer.parseQueryStringIntoComplexQuery("year-range:2018-2021");

        assertEquals(2018, transformer.getStartYear());
        assertEquals(2021, transformer.getEndYear());
    }
}
