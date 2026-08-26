package org.jabref.logic.importer.fetcher.transformers;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class BaseSearchQueryTransformer extends AbstractQueryTransformer {

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
        return createKeyValuePair("dccreator", author);
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("dctitle", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return createKeyValuePair("dcsource", journalTitle);
    }

    @Override
    protected String handleYear(String yearRange) {
        String result = super.handleYearRange(yearRange);
        if (result.isEmpty()) {
            return result;
        }
        return "(%s)".formatted(result);
    }
}
