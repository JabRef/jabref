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
        return String.format("author:\"%s\"", author);
    }

    @Override
    protected String handleTitle(String title) {
        return String.format("allintitle:\"%s\"", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return String.format("source:\"%s\"", journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        startYear = Math.min(startYear, Integer.parseInt(year));
        endYear = Math.max(endYear, Integer.parseInt(year));
        return "";
    }

    @Override
    protected String handleYearRange(String yearRange) {
        String[] split = yearRange.split("-");
        startYear = Math.min(startYear, Integer.parseInt(split[0]));
        endYear = Math.max(endYear, Integer.parseInt(split[1]));
        return "";
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return String.format("\"%s\"", term);
    }

    public int getStartYear() {
        return startYear == Integer.MAX_VALUE ? Integer.MIN_VALUE : startYear;
    }

    public int getEndYear() {
        return endYear == Integer.MIN_VALUE ? Integer.MAX_VALUE : endYear;
    }
}
