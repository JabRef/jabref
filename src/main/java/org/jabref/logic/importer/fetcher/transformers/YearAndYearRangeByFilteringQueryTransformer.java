package org.jabref.logic.importer.fetcher.transformers;

/**
 * This is a query transformer for a fetcher, which does not support server-side filtering by year and year-range.
 * Thus, JabRef (as client) filters for years and year ranges on client-side.
 */
public abstract class YearAndYearRangeByFilteringQueryTransformer extends YearRangeByFilteringQueryTransformer {

    @Override
    protected String handleYear(String year) {
        startYear = Math.min(startYear, Integer.parseInt(year));
        endYear = Math.max(endYear, Integer.parseInt(year));
        return "";
    }
}
