package org.jabref.logic.importer.fetcher.transformers;

import org.jabref.model.strings.StringUtil;

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
        return StringUtil.quoteStringIfSpaceIsContained(author);
    }

    @Override
    protected String handleTitle(String title) {
        return StringUtil.quoteStringIfSpaceIsContained(title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return StringUtil.quoteStringIfSpaceIsContained(journalTitle);
    }
}
