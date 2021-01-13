package org.jabref.logic.importer.fetcher.transformators;

import java.util.Optional;

public class ArXivQueryTransformer extends AbstractQueryTransformer {
    // These can be used for filtering in post processing
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
        startYear = Math.min(startYear, Integer.parseInt(year));
        endYear = Math.max(endYear, Integer.parseInt(year));
        return year;
    }

    /**
     * Currently not supported
     */
    @Override
    protected String handleYearRange(String yearRange) {
        String[] split = yearRange.split("-");
        startYear = Math.min(startYear, Integer.parseInt(split[0]));
        endYear = Math.max(endYear, Integer.parseInt(split[1]));
        return "";
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return String.format("all:\"%s\"", term);
    }

    public Optional<Integer> getStartYear() {
        return startYear == Integer.MAX_VALUE ? Optional.empty() : Optional.of(startYear);
    }

    public Optional<Integer> getEndYear() {
        return endYear == Integer.MIN_VALUE ? Optional.empty() : Optional.of(endYear);
    }
}
