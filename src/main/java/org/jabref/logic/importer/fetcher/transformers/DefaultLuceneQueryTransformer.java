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
        return handleOtherField("author", author).get();
    }

    @Override
    protected String handleTitle(String title) {
        return handleOtherField("title", title).get();
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return handleOtherField("journal", journalTitle).get();
    }

    @Override
    protected String handleYear(String year) {
        return handleOtherField("year", year).get();
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return "\"" + term + "\"";
    }
}
