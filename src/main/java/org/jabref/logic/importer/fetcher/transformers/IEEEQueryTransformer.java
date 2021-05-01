package org.jabref.logic.importer.fetcher.transformers;

import java.util.Objects;
import java.util.Optional;

/**
 * Needs to be instantiated for each new query
 */
public class IEEEQueryTransformer extends YearRangeByFilteringQueryTransformer {
    // These have to be integrated into the IEEE query URL as these are just supported as query parameters
    // Journal is wrapped in quotes by the transformer
    private String journal;
    private String articleNumber;
    private int startYear = Integer.MAX_VALUE;
    private int endYear = Integer.MIN_VALUE;

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
    protected String handleJournal(String journalTitle) {
        return handleUnFieldedTerm(journalTitle);
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

    private Optional<String> handleArticleNumber(String term) {
        articleNumber = term;
        return Optional.empty();
    }

    public Optional<String> getJournal() {
        return Objects.isNull(journal) ? Optional.empty() : Optional.of(journal);
    }

    public Optional<String> getArticleNumber() {
        return Objects.isNull(articleNumber) ? Optional.empty() : Optional.of(articleNumber);
    }
}
