package org.jabref.logic.importer.fetcher.transformers;

import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ISIDOREQueryTransformer extends  YearRangeByFilteringQueryTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ISIDOREQueryTransformer.class);

    @Override
    protected String getLogicalAndOperator() {
        return "+AND+";
    }

    @Override
    protected String getLogicalOrOperator() {
        return "+OR+";
    }

    @Override
    protected String getLogicalNotOperator() {
        return "+NOT+";
    }

    @Override
    protected String handleAuthor(String author) {
        return createKeyValuePair("author", author.replace(" ", "_"), "=" );
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("title", StringUtil.quoteStringIfSpaceIsContained(title));
    }

    @Override
    protected String handleJournal(String journalTitle) {
        LOGGER.warn("ISIDORE does not support searching by journal");
        return " ";
    }

    @Override
    protected String handleYear(String year) {
        return createKeyValuePair("date",year, "=");
    }
}
