package org.jabref.logic.importer.fetcher.transformers;

public class CollectionOfComputerScienceBibliographiesQueryTransformer extends AbstractQueryTransformer {

    @Override
    protected String getLogicalAndOperator() {
        return " ";
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
        return createKeyValuePair("au", author);
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("ti", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return quoteStringIfSpaceIsContained(journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        return String.format("year:%s", year);
    }
}
