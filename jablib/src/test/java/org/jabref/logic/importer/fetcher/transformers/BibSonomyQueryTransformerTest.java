package org.jabref.logic.importer.fetcher.transformers;

class BibSonomyQueryTransformerTest extends YearAndYearRangeByFilteringQueryTransformerTest<BibSonomyQueryTransformer> {
    @Override
    protected BibSonomyQueryTransformer getTransformer() {
        return new BibSonomyQueryTransformer();
    }
}
