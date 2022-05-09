package org.jabref.logic.importer.fetcher.transformers;

public class BiodiversityLibraryTransformer extends AbstractQueryTransformer {

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
        return " NOT ";
    }

    @Override
    protected String handleAuthor(String author) {
        return author;
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
