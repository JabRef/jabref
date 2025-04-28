package org.jabref.logic.importer.fetcher.transformers;

/**
 * This class extends the AbstractQueryTransformer to provide specific implementations
 * for transforming standard queries into ones suitable for the Scholar Archive's unique format.
 */
public class ScholarArchiveQueryTransformer extends AbstractQueryTransformer {

    @Override
    protected String getLogicalAndOperator() {
        return " AND ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return " OR ";
    }

    @Override
    protected String getLogicalNotOperator() {
        return "NOT ";
    }

    @Override
    protected String handleAuthor(String author) {
        return createKeyValuePair("contrib_names", author);
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("title", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return createKeyValuePair("container_name", journalTitle);
    }

    /**
     * Handles the year query by formatting it specifically for a range search in the Scholar Archive.
     * This method is for an exact year match.
     *
     * @param year the publication year to be searched in the Scholar Archive.
     * @return A string query segment formatted for the year search.
     */
    @Override
    protected String handleYear(String year) {
        return "publication.startDate:[" + year + " TO " + year + "]";
    }

    /**
     * Handles a year range query, transforming it for the Scholar Archive's query format.
     * If only a start year is provided, the range will extend to the current year.
     *
     * @param yearRange the range of years to be searched in the Scholar Archive, usually in the format "startYear-endYear".
     * @return A string query segment formatted for the year range search.
     */
    @Override
    protected String handleYearRange(String yearRange) {
        parseYearRange(yearRange);
        if (endYear == Integer.MAX_VALUE) {
            // If no specific end year is set, it assumes the range extends to the current year.
            return yearRange;
        }
        return "publication.startDate:[" + startYear + " TO " + endYear + "]";
    }
}




