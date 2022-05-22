package org.jabref.logic.importer.fetcher.transformers;

/**
 *
 * Medline/Pubmed specific transformer which uses suffixes for searches
 * see <a href="https://pubmed.ncbi.nlm.nih.gov/help/#search-tags">Pubmed help</a> for details
 *
 */
public class MedlineQueryTransformer extends AbstractQueryTransformer {

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
        return author + "[au]";
    }

    @Override
    protected String handleTitle(String title) {
        return title + "[ti]";
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return journalTitle + "[ta]";
    }

    @Override
    protected String handleYear(String year) {
        return year + "[dp]";
    }

    @Override
    protected String handleYearRange(String yearRange) {
        parseYearRange(yearRange);
        if (endYear == Integer.MAX_VALUE) {
            return yearRange;
        }
        return Integer.toString(startYear) + ":" + Integer.toString(endYear) + "[dp]";
    }
}
