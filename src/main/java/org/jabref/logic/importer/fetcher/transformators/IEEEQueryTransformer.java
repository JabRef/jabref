package org.jabref.logic.importer.fetcher.transformators;

/**
 * Needs to be instantiated for each new query
 */
public class IEEEQueryTransformer extends AbstractQueryTransformer {
    // These have to be integrated into the IEEE query URL as these are just supported as query parameters
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
        return "NOT ";
    }

    @Override
    protected String handleAuthor(String author) {
        return String.format("author:\"%s\"", author);
    }

    @Override
    protected String handleTitle(String title) {
        return String.format("article_title:\"%s\"", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return String.format("publication_title:\"%s\"", journalTitle);
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
        return String.format("\"%s\"", term);
    }

    public int getStartYear() {
        return startYear;
    }

    public int getEndYear() {
        return endYear;
    }
}
