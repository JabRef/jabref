package org.jabref.logic.importer.fetcher.transformers;

import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DBLP does not support explicit year field search, thus we extend YearAndYearRangeByFilteringQueryTransformer
 */
public class DBLPQueryTransformer extends YearAndYearRangeByFilteringQueryTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBLPQueryTransformer.class);

    @Override
    protected String getLogicalAndOperator() {
        return " ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return "|";
    }

    @Override
    protected String getLogicalNotOperator() {
        LOGGER.warn("DBLP does not support Boolean NOT operator.");
        return "";
    }

    @Override
    protected String handleAuthor(String author) {
        // DBLP does not support explicit author field search
        return StringUtil.quoteStringIfSpaceIsContained(author);
    }

    @Override
    protected String handleTitle(String title) {
        // DBLP does not support explicit title field search
        return StringUtil.quoteStringIfSpaceIsContained(title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        // DBLP does not support explicit journal field search
        return StringUtil.quoteStringIfSpaceIsContained(journalTitle);
    }
}
