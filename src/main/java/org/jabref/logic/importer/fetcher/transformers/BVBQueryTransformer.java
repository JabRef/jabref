package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

/**
 * Contextual Query Language Transformer for SRU in Bibliotheksverbund Bayern
 *
 * See <a href="https://www.loc.gov/standards/sru/cql/spec.html">Library of Congress specification</a>
 */
public class BVBQueryTransformer extends AbstractQueryTransformer {

    @Override
    protected String getLogicalAndOperator() {
        return " and ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return " or ";
    }

    @Override
    protected String getLogicalNotOperator() {
        return " not ";
    }

    @Override
    protected String handleAuthor(String author) {
        return String.format("marcxml.creator=\"%s\"", author);
    }

    @Override
    protected String handleTitle(String title) {
        return String.format("marcxml.title=\"%s\"", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return journalTitle;
    }

    @Override
    protected String handleYear(String year) {
        return year;
    }

    @Override
    protected String handleYearRange(String yearRange) {
        // Returns empty string as otherwise leads to no results
        return "";
    }

    @Override
    protected Optional<String> handleUnFieldedTerm(String term) {
        return Optional.of(createKeyValuePair("marcxml.all", term, "="));
    }

    @Override
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        return Optional.of("marcxml." + fieldAsString + "=\"" + term + "\"");
    }
}
