package org.jabref.logic.importer.fetcher.transformators;

import java.util.Optional;

public abstract class YearRangeByFilteringQueryTransformer extends AbstractQueryTransformer {

    // These can be used for filtering in post processing
    protected int startYear = Integer.MAX_VALUE;
    protected int endYear = Integer.MIN_VALUE;

    public Optional<Integer> getStartYear() {
        return startYear == Integer.MAX_VALUE ? Optional.empty() : Optional.of(startYear);
    }

    public Optional<Integer> getEndYear() {
        return endYear == Integer.MIN_VALUE ? Optional.empty() : Optional.of(endYear);
    }

    /**
     * The API does not support querying for a year range.
     * Nevertheless, we store the start year and end year,
     * because we filter it after fetching all results
     */
    @Override
    protected String handleYearRange(String yearRange) {
        String[] split = yearRange.split("-");
        startYear = Math.min(startYear, Integer.parseInt(split[0]));
        if (split.length >= 1) {
            endYear = Math.max(endYear, Integer.parseInt(split[1]));
        }
        return "";
    }

}
