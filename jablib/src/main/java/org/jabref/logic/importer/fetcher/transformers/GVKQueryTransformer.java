package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GVKQueryTransformer extends YearRangeByFilteringQueryTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GVKQueryTransformer.class);

    @Override
    protected String getLogicalAndOperator() {
        return " and ";
    }

    @Override
    protected String getLogicalOrOperator() {
        LOGGER.warn("GVK does not support Boolean OR operator");
        return " ";
    }

    @Override
    protected String getLogicalNotOperator() {
        LOGGER.warn("GVK does not support Boolean NOT operator");
        return " ";
    }

    @Override
    protected String handleAuthor(String author) {
        return createKeyValuePair("pica.per", author, "=");
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("pica.tit", title, "=");
    }

    @Override
    protected String handleJournal(String journalTitle) {
        // zti means "Zeitschrift", does not search for conferences (kon:)
        return createKeyValuePair("pica.zti", journalTitle, "=");
    }

    @Override
    protected String handleYear(String year) {
        // "erj" means "Erscheinungsjahr"
        return "pica.erj=" + year;
    }

    @Override
    protected Optional<String> handleUnFieldedTerm(String term) {
        // all does not search in full-text
        // Other option is txt: but this does not search in meta data
        return Optional.of(createKeyValuePair("pica.all", term, "="));
    }

    @Override
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        return Optional.of(createKeyValuePair("pica." + fieldAsString, term, "="));
    }
}
