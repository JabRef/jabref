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
        Optional<String> query = getTransformator().parseQueryStringIntoComplexQuery("year:2018");
        Optional<String> expected = Optional.of("2018");
        assertEquals(expected, query);
    }

    @Disabled("Year-range search is not supported by the arXiv API")
    @Override
    public void convertYearRangeField() throws Exception {

    }
}
