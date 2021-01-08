package org.jabref.logic.importer.fetcher.transformators;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DBLPQueryTransformerTest implements InfixTransformerTest {

    @Override
    public AbstractQueryTransformer getTransformator() {
        return new DBLPQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "";    }

    @Override
    public String getJournalPrefix() {
        return "";
    }

    @Override
    public String getTitlePrefix() {
        return "";
    }

    @Override
    public void convertYearField() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("year:2015");
        Optional<String> expected = Optional.of("2015");
        assertEquals(expected, searchQuery);
    }

    @Override
    public void convertYearRangeField() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("year-range:2012-2015");
        Optional<String> expected = Optional.of("2012|2013|2014|2015");
        assertEquals(expected, searchQuery);
    }
}
