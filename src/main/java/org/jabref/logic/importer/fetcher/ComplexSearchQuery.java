package org.jabref.logic.importer.fetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

public class ComplexSearchQuery {
    // Field for non-fielded search
    private final String defaultField;
    private final List<String> authors;
    private final List<String> titlePhrases;
    private final Integer fromYear;
    private final Integer toYear;
    private final Integer singleYear;
    private final String journal;

    private ComplexSearchQuery(String defaultField, List<String> authors, List<String> titlePhrases, Integer fromYear, Integer toYear, Integer singleYear, String journal) {
        this.defaultField = defaultField;
        this.authors = authors;
        this.titlePhrases = titlePhrases;
        this.fromYear = fromYear;
        // Some APIs do not support, or not fully support, year based search. In these cases, the non applicable parameters are ignored.
        this.toYear = toYear;
        this.journal = journal;
        this.singleYear = singleYear;
    }

    public Optional<String> getDefaultField() {
        return Optional.ofNullable(defaultField);
    }

    public Optional<List<String>> getAuthors() {
        return Optional.ofNullable(authors);
    }

    public Optional<List<String>> getTitlePhrases() {
        return Optional.ofNullable(titlePhrases);
    }

    public Optional<Integer> getFromYear() {
        return Optional.ofNullable(fromYear);
    }

    public Optional<Integer> getToYear() {
        return Optional.ofNullable(toYear);
    }

    public Optional<Integer> getSingleYear() {
        return Optional.ofNullable(singleYear);
    }

    public Optional<String> getJournal() {
        return Optional.ofNullable(journal);
    }

    public static ComplexSearchQueryBuilder builder() {
        return new ComplexSearchQueryBuilder();
    }

    public static class ComplexSearchQueryBuilder {
        private String defaultField;
        private List<String> authors;
        private List<String> titlePhrases;
        private String journal;
        private Integer fromYear;
        private Integer toYear;
        private Integer singleYear;

        public ComplexSearchQueryBuilder() {
        }

        public ComplexSearchQueryBuilder defaultField(String defaultField) {
            if (Objects.requireNonNull(defaultField).isBlank()) {
                throw new IllegalArgumentException("Parameter must not be blank");
            }
            this.defaultField = defaultField;
            return this;
        }

        /**
         * Adds author and wraps it in quotes
         */
        public ComplexSearchQueryBuilder author(String author) {
            if (Objects.requireNonNull(author).isBlank()) {
                throw new IllegalArgumentException("Parameter must not be blank");
            }
            if (Objects.isNull(authors)) {
                this.authors = new ArrayList<>();
            }
            // Strip all quotes before wrapping
            this.authors.add(String.format("\"%s\"", author.replace("\"", "")));
            return this;
        }

        /**
         * Adds title phrase and wraps it in quotes
         */
        public ComplexSearchQueryBuilder titlePhrase(String titlePhrase) {
            if (Objects.requireNonNull(titlePhrase).isBlank()) {
                throw new IllegalArgumentException("Parameter must not be blank");
            }
            if (Objects.isNull(titlePhrases)) {
                this.titlePhrases = new ArrayList<>();
            }
            // Strip all quotes before wrapping
            this.titlePhrases.add(String.format("\"%s\"", titlePhrase.replace("\"", "")));
            return this;
        }

        public ComplexSearchQueryBuilder fromYearAndToYear(Integer fromYear, Integer toYear) {
            if (Objects.nonNull(singleYear)) {
                throw new IllegalArgumentException("You can not use single year and year range search.");
            }
            this.fromYear = Objects.requireNonNull(fromYear);
            this.toYear = Objects.requireNonNull(toYear);
            return this;
        }

        public ComplexSearchQueryBuilder singleYear(Integer singleYear) {
            if (Objects.nonNull(fromYear) || Objects.nonNull(toYear)) {
                throw new IllegalArgumentException("You can not use single year and year range search.");
            }
            this.singleYear = Objects.requireNonNull(singleYear);
            return this;
        }

        public ComplexSearchQueryBuilder journal(String journal) {
            if (Objects.requireNonNull(journal).isBlank()) {
                throw new IllegalArgumentException("Parameter must not be blank");
            }
            this.journal = String.format("\"%s\"", journal.replace("\"", ""));
            return this;
        }

        /**
         * Instantiates the AdvancesSearchConfig from the provided Builder parameters
         * If all text fields are empty an empty optional is returned
         *
         * @return AdvancedSearchConfig instance with the fields set to the values defined in the building instance.
         * @throws IllegalStateException An IllegalStateException is thrown in case all text search fields are empty.
         *                               See: https://softwareengineering.stackexchange.com/questions/241309/builder-pattern-when-to-fail/241320#241320
         */
        public ComplexSearchQuery build() throws IllegalStateException {
            if (textSearchFieldsAndYearFieldsAreEmpty()) {
                throw new IllegalStateException("At least one text field has to be set");
            }
            return new ComplexSearchQuery(defaultField, authors, titlePhrases, fromYear, toYear, singleYear, journal);
        }

        private boolean textSearchFieldsAndYearFieldsAreEmpty() {
            return StringUtil.isBlank(defaultField) && this.stringListIsBlank(titlePhrases) &&
                    this.stringListIsBlank(authors) && StringUtil.isBlank(journal) && yearFieldsAreEmpty();
        }

        private boolean yearFieldsAreEmpty() {
            return Objects.isNull(singleYear) && Objects.isNull(fromYear) && Objects.isNull(toYear);
        }

        private boolean stringListIsBlank(List<String> stringList) {
            return Objects.isNull(stringList) || stringList.stream().allMatch(String::isBlank);
        }
    }
}
