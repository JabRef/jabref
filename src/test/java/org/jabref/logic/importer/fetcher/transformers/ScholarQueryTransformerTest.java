package org.jabref.logic.importer.fetcher.transformers;

class ScholarQueryTransformerTest extends YearAndYearRangeByFilteringQueryTransformerTest<ScholarQueryTransformer> {

    @Override
    public ScholarQueryTransformer getTransformer() {
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

}
