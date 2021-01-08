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
        return String.format("au:\"%s\"", author);
    }

    @Override
    protected String handleTitle(String title) {
        return String.format("ti:\"%s\"", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return String.format("jr:\"%s\"", journalTitle);
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
        return String.format("all:\"%s\"", term);
    }
}
