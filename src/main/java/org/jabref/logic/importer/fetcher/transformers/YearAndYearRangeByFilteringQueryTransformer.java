package org.jabref.logic.importer.fetcher.transformers;

public abstract class YearAndYearRangeByFilteringQueryTransformer extends YearRangeByFilteringQueryTransformer {

    @Override
    protected String handleYear(String year) {
        startYear = Math.min(startYear, Integer.parseInt(year));
        endYear = Math.max(endYear, Integer.parseInt(year));
        return "";
    }

}
