package org.jabref.logic.importer.fetcher.transformers;

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
        return createKeyValuePair("au", author);
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("ti", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return createKeyValuePair("pt", journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        return "sd:" + year + getLogicalAndOperator() + "ed: " + year;
    }

    @Override
    protected String handleYearRange(String yearRange) {
        parseYearRange(yearRange);
        if (endYear == Integer.MAX_VALUE) {
            return yearRange;
        }
        return "sd:" + Integer.toString(startYear) + getLogicalAndOperator() + "ed:" + Integer.toString(endYear);
    }
}
