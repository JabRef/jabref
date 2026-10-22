package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.util.strings.StringUtil;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Translates JabRef's search query into BibSonomy's free-text `search` parameter.
/// BibSonomy has no fielded search and no `OR`; years are filtered client-side.
@NullMarked
public class BibSonomyQueryTransformer extends YearAndYearRangeByFilteringQueryTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibSonomyQueryTransformer.class);

    @Override
    protected String getLogicalAndOperator() {
        return " ";
    }

    @Override
    protected String getLogicalOrOperator() {
        LOGGER.warn("BibSonomy does not support the Boolean OR operator, using AND instead.");
        return " ";
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
