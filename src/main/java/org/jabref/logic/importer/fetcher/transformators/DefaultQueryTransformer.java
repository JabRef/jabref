package org.jabref.logic.importer.fetcher.transformators;

/**
 * Default query transformer without any boolean operators
 */
public class DefaultQueryTransformer extends YearAndYearRangeByFilteringQueryTransformer {

    @Override
    protected String getLogicalAndOperator() {
        return " ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return " ";
    }

    @Override
    protected String getLogicalNotOperator() {
        return "";
    }

    @Override
    protected String handleAuthor(String author) {
        return author;
    }

    @Override
    protected String handleTitle(String title) {
        return title;
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return journalTitle;
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return term;
    }
}
