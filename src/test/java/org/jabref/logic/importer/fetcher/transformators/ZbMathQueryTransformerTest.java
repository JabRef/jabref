package org.jabref.logic.importer.fetcher.transformators;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZbMathQueryTransformerTest implements InfixTransformerTest{

    @Override
    public AbstractQueryTransformer getTransformator() {
        return new ZbMathQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "au:";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "any:";
    }

    @Override
    public String getJournalPrefix() {
        return "so:";
    }

    @Override
    public String getTitlePrefix() {
        return "ti:";
    }

    @Override
    public void convertYearField() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("year:2015");
        Optional<String> expected = Optional.of("py:2015");
        assertEquals(expected, searchQuery);
    }

    @Override
    public void convertYearRangeField() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("year-range:2012-2015");
        Optional<String> expected = Optional.of("py:2012-2015");
        assertEquals(expected, searchQuery);
    }
}
