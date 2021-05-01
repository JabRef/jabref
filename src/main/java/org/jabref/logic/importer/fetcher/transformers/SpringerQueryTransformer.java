package org.jabref.logic.importer.fetcher.transformers;

/**
 * This class converts a query string written in lucene syntax into a complex  query.
 *
 * For simplicity this is currently limited to fielded data and the boolean AND operator.
 */
public class SpringerQueryTransformer extends AbstractQueryTransformer {

    @Override
    public String getLogicalAndOperator() {
        return " AND ";
    }

    @Override
    public String getLogicalOrOperator() {
        return " OR ";
    }

    @Override
    protected String getLogicalNotOperator() {
        return "-";
    }

    @Override
    protected String handleAuthor(String author) {
        return createKeyValuePair("name", author);
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
        return String.format("date:%s*", year);
    }
}
