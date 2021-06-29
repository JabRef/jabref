package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

public class ArXivQueryTransformer extends YearRangeByFilteringQueryTransformer {
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
        return " ANDNOT ";
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
        return createKeyValuePair("jr", journalTitle);
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

    @Override
    protected Optional<String> handleUnFieldedTerm(String term) {
        return Optional.of(createKeyValuePair("all", term));
    }

}
