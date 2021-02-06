package org.jabref.logic.importer.fetcher.transformators;

public class ZbMathQueryTransformer extends AbstractQueryTransformer {

    @Override
    protected String getLogicalAndOperator() {
        return " & ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return " | ";
    }

    @Override
    protected String getLogicalNotOperator() {
        return "!";
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
        return String.format("so:\"%s\"", journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        return "py:" + year;
    }

    @Override
    protected String handleYearRange(String yearRange) {
        return "py:" + yearRange;
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return String.format("any:\"%s\"", term);
    }
}
