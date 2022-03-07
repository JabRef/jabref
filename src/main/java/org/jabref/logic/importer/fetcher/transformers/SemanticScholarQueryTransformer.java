package org.jabref.logic.importer.fetcher.transformers;

public class SemanticScholarQueryTransformer extends AbstractQueryTransformer {
    @Override
    protected String getLogicalAndOperator() {
        return null;
    }

    @Override
    protected String getLogicalOrOperator() {
        return null;
    }

    @Override
    protected String getLogicalNotOperator() {
        return null;
    }

    @Override
    protected String handleAuthor(String author) {
        return null;
    }

    @Override
    protected String handleTitle(String title) {
        return null;
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return null;
    }

    @Override
    protected String handleYear(String year) {
        return null;
    }
}
