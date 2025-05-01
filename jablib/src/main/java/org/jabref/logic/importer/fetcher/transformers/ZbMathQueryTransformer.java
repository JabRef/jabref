package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

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
        return createKeyValuePair("au", author);
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("ti", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return createKeyValuePair("so", journalTitle);
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
    protected Optional<String> handleUnFieldedTerm(String term) {
        return Optional.of(createKeyValuePair("any", term));
    }
}
