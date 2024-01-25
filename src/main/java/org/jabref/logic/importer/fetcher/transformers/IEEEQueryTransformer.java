package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.formatter.casechanger.Word;
import org.jabref.model.strings.StringUtil;

/**
 * Needs to be instantiated for each new query
 *
 * Stop words are ignored. See ADR-0022.
 */
public class IEEEQueryTransformer extends YearRangeByFilteringQueryTransformer {
    // These have to be integrated into the IEEE query URL as these are just supported as query parameters
    // Journal is wrapped in quotes by the transformer
    private String journal;
    private String articleNumber;

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
        return "NOT ";
    }

    @Override
    protected String handleAuthor(String author) {
        return createKeyValuePair("author", author);
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("article_title", title);
    }

    @Override
    protected String handleJournal(String journal) {
        this.journal = journal;
        return StringUtil.quoteStringIfSpaceIsContained(journal);
    }

    @Override
    protected String handleYear(String year) {
        startYear = Math.min(startYear, Integer.parseInt(year));
        endYear = Math.max(endYear, Integer.parseInt(year));
        return "";
    }

    @Override
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        return switch (fieldAsString) {
            case "article_number" -> handleArticleNumber(term);
            default -> super.handleOtherField(fieldAsString, term);
        };
    }

    @Override
    protected Optional<String> handleUnFieldedTerm(String term) {
        if (Word.SMALLER_WORDS.contains(term)) {
            return Optional.empty();
        }
        return super.handleUnFieldedTerm(term);
    }

    private Optional<String> handleArticleNumber(String term) {
        articleNumber = term;
        return Optional.empty();
    }

    public Optional<String> getJournal() {
        return journal == null ? Optional.empty() : Optional.of(journal);
    }

    public Optional<String> getArticleNumber() {
        return articleNumber == null ? Optional.empty() : Optional.of(articleNumber);
    }
}
