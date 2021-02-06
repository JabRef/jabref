package org.jabref.logic.importer.fetcher.transformators;

import java.util.Objects;
import java.util.Optional;

/**
 * Needs to be instantiated for each new query
 */
public class IEEEQueryTransformer extends AbstractQueryTransformer {
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
        return String.format("author:\"%s\"", author);
    }

    @Override
    protected String handleTitle(String title) {
        return String.format("article_title:\"%s\"", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        journal = String.format("\"%s\"", journalTitle);
        return "";
    }

    @Override
    protected String handleYear(String year) {
        startYear = Math.min(startYear, Integer.parseInt(year));
        endYear = Math.max(endYear, Integer.parseInt(year));
        return "";
    }

    @Override
    protected String handleYearRange(String yearRange) {
        String[] split = yearRange.split("-");
        startYear = Math.min(startYear, Integer.parseInt(split[0]));
        endYear = Math.max(endYear, Integer.parseInt(split[1]));
        return "";
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return String.format("\"%s\"", term);
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

    public Optional<Integer> getStartYear() {
        return startYear == Integer.MAX_VALUE ? Optional.empty() : Optional.of(startYear);
    }

    public Optional<Integer> getEndYear() {
        return endYear == Integer.MIN_VALUE ? Optional.empty() : Optional.of(endYear);
    }

    public Optional<String> getJournal() {
        return Objects.isNull(journal) ? Optional.empty() : Optional.of(journal);
    }

    public Optional<String> getArticleNumber() {
        return Objects.isNull(articleNumber) ? Optional.empty() : Optional.of(articleNumber);
    }
}
