package org.jabref.logic.importer.fetcher.transformers;

/**
 * This class extends the AbstractQueryTransformer to provide specific implementations
 * for transforming standard queries into ones suitable for the Scholar Archive's unique format.
 */
public class ScholarArchiveQueryTransformer extends AbstractQueryTransformer {

    /**
     * Returns the operator for logical "AND" used in the Scholar Archive query language.
     *
     * @return A string representing the logical "AND" operator.
     */
    @Override
    protected String getLogicalAndOperator() {
        return " AND ";
    }

    /**
     * Returns the operator for logical "OR" used in the Scholar Archive query language.
     *
     * @return A string representing the logical "OR" operator.
     */
    @Override
    protected String getLogicalOrOperator() {
        return " OR ";
    }

    /**
     * Returns the operator for logical "NOT" used in the Scholar Archive query language.
     *
     * @return A string representing the logical "NOT" operator.
     */
    @Override
    protected String getLogicalNotOperator() {
        return "NOT ";
    }

    /**
     * Transforms the author query segment into a 'contrib_names' key-value pair for the Scholar Archive query.
     *
     * @param author the author's name to be searched in the Scholar Archive.
     * @return A string query segment representing the author search criterion.
     */
    @Override
    protected String handleAuthor(String author) {
        return createKeyValuePair("contrib_names", author);
    }

    /**
     * Transforms the title query segment into a 'title' key-value pair for the Scholar Archive query.
     *
     * @param title the title of the work to be searched in the Scholar Archive.
     * @return A string query segment representing the title search criterion.
     */
    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("title", title);
    }

    /**
     * Transforms the journal title query segment into a 'container_name' key-value pair for the Scholar Archive query.
     *
     * @param journalTitle the name of the journal to be searched in the Scholar Archive.
     * @return A string query segment representing the journal title search criterion.
     */
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
        // This method presumably parses the year range into individual components.
        parseYearRange(yearRange);
        if (endYear == Integer.MAX_VALUE) {
            // If no specific end year is set, it assumes the range extends to the current year.
            return yearRange;
        }
        // Formats the year range for inclusion in the Scholar Archive query.
        return "publication.startDate:[" + startYear + " TO " + endYear + "]";
    }
}




