package org.jabref.logic.importer.fetcher.transformers;

class DefaultQueryTransformerTest extends YearAndYearRangeByFilteringQueryTransformerTest<DefaultQueryTransformer> {

    @Override
    protected DefaultQueryTransformer getTransformer() {
        return new DefaultQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "";
    }

    @Override
    public String getJournalPrefix() {
        return "";
    }

    @Override
    public String getTitlePrefix() {
        return "";
    }
}
