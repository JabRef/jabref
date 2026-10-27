package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class ArXivQueryTransformer extends YearRangeByFilteringQueryTransformer {
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
        return " ANDNOT ";
    }

    @Override
    protected String handleAuthor(String author) {
        return createKeyValuePair("au", author);
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("ti", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return createKeyValuePair("jr", journalTitle);
    }

    /// Manual testing shows that this works if added as an unfielded term, might lead to false positives
    @Override
    protected String handleYear(String year) {
        startYear = Math.min(startYear, Integer.parseInt(year));
        endYear = Math.max(endYear, Integer.parseInt(year));
        return year;
    }

    @Override
    protected Optional<String> handleUnFieldedTerm(String term) {
        return Optional.of(createKeyValuePair("all", term));
    }

    public Optional<String> entryToQuery(BibEntry entry) {
        // Entry-based arXiv lookup accepts partial author strings. Keep this looser than handleAuthor(), which quotes
        // multi-word author searches for structured query-node transformations.
        Optional<String> authorQuery = entry.getField(StandardField.AUTHOR).map(author -> "au:" + author);
        Optional<String> titleQuery = entry.getField(StandardField.TITLE)
                                           .map(StringUtil::ignoreCurlyBracket)
                                           .map(this::handleTitle);
        var queryTerms = Stream.of(authorQuery, titleQuery)
                               .flatMap(Optional::stream)
                               .toList();

        if (queryTerms.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(getLogicalAndOperator(), queryTerms));
    }
}
