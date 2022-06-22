package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

/**
 * This is a query transformer for a fetcher, which does not support server-side filtering by year-range (e.g., only publications between 1999 and 2002).
 * Thus, JabRef (as client) filters for year ranges on client-side.
 */
public abstract class YearRangeByFilteringQueryTransformer extends AbstractQueryTransformer {

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
     *
     * @return "", because the provider does not support server-side filtering, but our client filters
     */
    @Override
    protected String handleYearRange(String yearRange) {
        parseYearRange(yearRange);
        return "";
    }
}
