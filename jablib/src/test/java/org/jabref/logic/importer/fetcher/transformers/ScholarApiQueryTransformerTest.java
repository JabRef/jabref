package org.jabref.logic.importer.fetcher.transformers;

class ScholarApiQueryTransformerTest extends YearAndYearRangeByFilteringQueryTransformerTest<ScholarApiQueryTransformer> {

    @Override
    public ScholarApiQueryTransformer getTransformer() {
        return new ScholarApiQueryTransformer();
    }
}
