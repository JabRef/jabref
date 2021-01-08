package org.jabref.logic.importer.fetcher.transformators;

import java.util.Optional;

import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.*;

class GVKQueryTransformerTest implements InfixTransformerTest{

    @Override
    public AbstractQueryTransformer getTransformator() {
        return new GVKQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "per:";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "all:";
    }

    @Override
    public String getJournalPrefix() {
        return "zti:";
    }

    @Override
    public String getTitlePrefix() {
        return "tit:";
    }

    @Override
    public void convertYearField() throws Exception {
        Optional<String> query = getTransformator().parseQueryStringIntoComplexQuery("year:2018");

        Optional<String> expected = Optional.of("ver:2018");
        assertEquals(expected, query);
    }

    @Disabled("Not supported by GVK")
    @Override
    public void convertYearRangeField() throws Exception {

    }
}
