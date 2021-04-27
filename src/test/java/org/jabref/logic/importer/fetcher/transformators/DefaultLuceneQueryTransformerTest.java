package org.jabref.logic.importer.fetcher.transformators;

class DefaultLuceneQueryTransformerTest extends YearAndYearRangeByFilteringQueryTransformerTest<DefaultQueryTransformer> {

    @Override
    protected DefaultQueryTransformer getTransformer() {
        return new DefaultQueryTransformer();
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
        return "journal:";
    }

    @Override
    public String getTitlePrefix() {
        return "title:";
    }
}
