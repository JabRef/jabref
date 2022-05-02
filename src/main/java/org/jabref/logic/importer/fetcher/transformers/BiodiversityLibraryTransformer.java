package org.jabref.logic.importer.fetcher.transformers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

public class BiodiversityLibraryTransformer extends YearRangeByFilteringQueryTransformer {

    private static final List<String> STOP_WORDS = List.of("a", "and", "for", "or", "with");

    private String journal;
    private String articleNumber;
    private String author;
    private String title;

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
        return " NOT ";
    }

    @Override
    protected String handleAuthor(String author) {
        this.author = author;
        return StringUtil.quoteStringIfSpaceIsContained(author);
    }

    @Override
    protected String handleTitle(String title) {
        this.title = title;
        return StringUtil.quoteStringIfSpaceIsContained(title);
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
        if (STOP_WORDS.contains(term)) {
            return Optional.empty();
        }
        return super.handleUnFieldedTerm(term);
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

    public Optional<String> getTitle() {
        return Objects.isNull(title) ? Optional.empty() : Optional.of(title);
    }

    public Optional<String> getAuthor() {
        return Objects.isNull(author) ? Optional.empty() : Optional.of(author);
    }
}
