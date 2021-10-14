package org.jabref.logic.importer.fetcher.transformers;

/**
 * Transforms the query to a lucene query string
 */
public class DefaultLuceneQueryTransformer extends AbstractQueryTransformer {

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
        return createKeyValuePair("author", author);
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("title", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return createKeyValuePair("journal", journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        return createKeyValuePair("year", year);
    }
}
