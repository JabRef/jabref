package org.jabref.logic.importer.fetcher.transformators;

public class ScholarQueryTransformer extends AbstractQueryTransformer {
    // These have to be integrated into the Google Scholar query URL as these are just supported as query parameters
    private int startYear = Integer.MAX_VALUE;
    private int endYear = Integer.MIN_VALUE;

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
        return "-";
    }

    @Override
    protected String handleAuthor(String author) {
        return "author:" + author;
    }

    @Override
    protected String handleTitle(String title) {
        return "allintitle:" + title;
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return "source:" + journalTitle;
    }

    @Override
    protected String handleYear(String year) {
        startYear = Math.max(startYear, Integer.parseInt(year));
        endYear = Math.max(endYear, Integer.parseInt(year));
        return "";
    }

    @Override
    protected String handleYearRange(String yearRange) {
        String[] split = yearRange.split("-");
        startYear = Math.max(startYear, Integer.parseInt(split[0]));
        endYear = Math.max(endYear, Integer.parseInt(split[1]));
        return "";
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return term;
    }

    public int getStartYear() {
        return startYear;
    }

    public int getEndYear() {
        return endYear;
    }
}
