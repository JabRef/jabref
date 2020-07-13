package org.jabref.logic.importer.fetcher;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

public class AdvancedSearchConfig {
    private final String defaultField;
    private final String author;
    private final String title;
    private final Integer fromYear;
    private final Integer toYear;
    private final String journal;

    private AdvancedSearchConfig(String defaultField, String author, String title, Integer fromYear, Integer toYear, String journal) {
        this.defaultField = defaultField;
        this.author = author;
        this.title = title;
        this.fromYear = fromYear;
        this.toYear = toYear;
        this.journal = journal;
    }

    public Optional<String> getDefaultField() {
        return Optional.ofNullable(defaultField);
    }

    public Optional<String> getAuthor() {
        return Optional.ofNullable(author);
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    public Optional<Integer> getFromYear() {
        return Optional.ofNullable(fromYear);
    }

    public Optional<Integer> getToYear() {
        return Optional.ofNullable(toYear);
    }

    public Optional<String> getJournal() {
        return Optional.ofNullable(journal);
    }

    public static AdvancedSearchConfigBuilder builder() {
        return new AdvancedSearchConfigBuilder();
    }

    public static class AdvancedSearchConfigBuilder {
        private String defaultField;
        private String author;
        private String title;
        private String journal;
        private Integer fromYear;
        private Integer toYear;

        public AdvancedSearchConfigBuilder() {
        }

        public AdvancedSearchConfigBuilder defaultField(String defaultField) {
            if (Objects.requireNonNull(defaultField).isBlank()) {
                throw new IllegalArgumentException("Parameter must not be blank");
            }
            this.defaultField = defaultField;
            return this;
        }

        public AdvancedSearchConfigBuilder author(String author) {
            if (Objects.requireNonNull(author).isBlank()) {
                throw new IllegalArgumentException("Parameter must not be blank");
            }
            this.author = author;
            return this;
        }

        public AdvancedSearchConfigBuilder title(String title) {
            if (Objects.requireNonNull(title).isBlank()) {
                throw new IllegalArgumentException("Parameter must not be blank");
            }
            this.title = title;
            return this;
        }

        public AdvancedSearchConfigBuilder fromYear(Integer fromYear) {
            this.fromYear = Objects.requireNonNull(fromYear);
            return this;
        }

        public AdvancedSearchConfigBuilder toYear(Integer toYear) {
            this.toYear = Objects.requireNonNull(toYear);
            return this;
        }

        public AdvancedSearchConfigBuilder journal(String journal) {
            if (Objects.requireNonNull(journal).isBlank()) {
                throw new IllegalArgumentException("Parameter must not be blank");
            }
            this.journal = journal;
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
        public AdvancedSearchConfig build() throws IllegalStateException {
            if (textSearchFieldsAreEmpty()) {
                throw new IllegalStateException("At least one text field has to be set");
            }
            return new AdvancedSearchConfig(defaultField, author, title, fromYear, toYear, journal);
        }

        private boolean textSearchFieldsAreEmpty() {
            return StringUtil.isBlank(defaultField) && StringUtil.isBlank(title) && StringUtil.isBlank(author) && StringUtil.isBlank(journal);
        }
    }
}
