package org.jabref.logic.importer.fetcher.transformators;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GVKQueryTransformer extends AbstractQueryTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GVKQueryTransformer.class);

    @Override
    protected String getLogicalAndOperator() {
        return " and ";
    }

    @Override
    protected String getLogicalOrOperator() {
        LOGGER.warn("GVK does not support Boolean OR operator");
        return "";
    }

    @Override
    protected String getLogicalNotOperator() {
        LOGGER.warn("GVK does not support Boolean NOT operator");
        return "";
    }

    @Override
    protected String handleAuthor(String author) {
        return String.format("pica.per=\"%s\"", author);
    }

    @Override
    protected String handleTitle(String title) {
        return String.format("pica.tit=\"%s\"", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        // zti means "Zeitschrift", does not search for conferences (kon:)
        return String.format("pica.zti=\"%s\"", journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        // ver means Veröffentlichungsangaben
        return "pica.ver=" + year;
    }

    @Override
    protected String handleYearRange(String yearRange) {
        // Returns empty string as otherwise leads to no results
        return "";
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        // all does not search in full-text
        // Other option is txt: but this does not search in meta data
        return String.format("pica.all=\"%s\"", term);
    }

    @Override
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        return Optional.of("pica." + fieldAsString + "=\"" + term + "\"");
    }
}
