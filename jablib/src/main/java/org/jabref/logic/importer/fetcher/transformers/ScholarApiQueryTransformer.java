package org.jabref.logic.importer.fetcher.transformers;

import org.jabref.logic.util.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScholarApiQueryTransformer extends YearAndYearRangeByFilteringQueryTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScholarApiQueryTransformer.class);

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
        // ScholarApi does not support explicit author field search
        return StringUtil.quoteStringIfSpaceIsContained(author);
    }

    @Override
    protected String handleTitle(String title) {
        // ScholarApi does not support explicit title field search
        return StringUtil.quoteStringIfSpaceIsContained(title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        LOGGER.warn("ScholarAPI has no journal scoped search");
        return "";
    }
}
