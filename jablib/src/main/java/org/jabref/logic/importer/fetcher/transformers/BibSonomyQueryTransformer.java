package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.util.strings.StringUtil;

import org.jspecify.annotations.NullMarked;

/// Translates JabRef's search query into BibSonomy's free-text `search` parameter.
/// BibSonomy has no fielded search; years are filtered client-side.
@NullMarked
public class BibSonomyQueryTransformer extends YearAndYearRangeByFilteringQueryTransformer {

    @Override
    protected String getLogicalAndOperator() {
        return " ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return " OR ";
    }

    @Override
    protected String getLogicalNotOperator() {
        return "-";
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

    @Override
    protected String handleDoi(String term) {
        // BibSonomy's full-text search already covers DOIs
        return StringUtil.quoteStringIfSpaceIsContained(term);
    }

    @Override
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        // Tags (keywords=…) and any other field become plain search terms
        return Optional.of(StringUtil.quoteStringIfSpaceIsContained(term));
    }
}
