package org.jabref.logic.importer.fetcher.transformators;

public class JstorQueryTransformer extends AbstractQueryTransformer {
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
        return String.format("au:\"%s\"", author);
    }

    @Override
    protected String handleTitle(String title) {
        return String.format("ti:\"%s\"", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return String.format("pt:\"%s\"", journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        return "sd:" + year + getLogicalAndOperator() + "ed: " + year;
    }

    @Override
    protected String handleYearRange(String yearRange) {
        String[] split = yearRange.split("-");
        return "sd:" + split[0] + getLogicalAndOperator() + "ed:" + split[1];
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return String.format("\"%s\"", term);
    }
}
