package org.jabref.logic.importer.fetcher.transformators;

public class ArXivQueryTransformer extends AbstractQueryTransformer {
    @Override
    protected String getLogicalAndOperator() {
        return " AND ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return " OR ";
    }

    /**
     * Check whether this works as an unary operator
     * @return
     */
    @Override
    protected String getLogicalNotOperator() {
        return " ANDNOT ";
    }

    @Override
    protected String handleAuthor(String author) {
        return "au:" + author;
    }

    @Override
    protected String handleTitle(String title) {
        return "ti:" + title;
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return "jr:" + journalTitle;
    }

    /**
     * Manual testing shows that this works if added as an unfielded term, might lead to false positives
     */
    @Override
    protected String handleYear(String year) {
        return year;
    }

    /**
     * Currently not supported
     */
    @Override
    protected String handleYearRange(String yearRange) {
        return "";
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return "all:" + term;
    }
}
