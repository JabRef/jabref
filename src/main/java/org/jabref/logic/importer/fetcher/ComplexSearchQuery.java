package org.jabref.logic.importer.fetcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

import org.apache.lucene.index.Term;

public class ComplexSearchQuery {
    // Field for non-fielded search
    private final List<String> defaultField;
    private final List<String> authors;
    private final List<String> titlePhrases;
    private final Integer fromYear;
    private final Integer toYear;
    private final Integer singleYear;
    private final String journal;

    private ComplexSearchQuery(List<String> defaultField, List<String> authors, List<String> titlePhrases, Integer fromYear, Integer toYear, Integer singleYear, String journal) {
        this.defaultField = defaultField;
        this.authors = authors;
        this.titlePhrases = titlePhrases;
        this.fromYear = fromYear;
        // Some APIs do not support, or not fully support, year based search. In these cases, the non applicable parameters are ignored.
        this.toYear = toYear;
        this.journal = journal;
        this.singleYear = singleYear;
    }

    public static ComplexSearchQuery fromTerms(Collection<Term> terms) {
        ComplexSearchQueryBuilder builder = ComplexSearchQuery.builder();
        terms.forEach(term -> {
            String termText = term.text();
            switch (term.field().toLowerCase()) {
                case "author" -> builder.author(termText);
                case "title" -> builder.titlePhrase(termText);
                case "journal" -> builder.journal(termText);
                case "year" -> builder.singleYear(Integer.valueOf(termText));
                case "year-range" -> builder.parseYearRange(termText);
                case "default" -> builder.defaultFieldPhrase(termText);
            }
        });
        return builder.build();
    }

    public List<String> getDefaultFieldPhrases() {
        return defaultField;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public List<String> getTitlePhrases() {
        return titlePhrases;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ComplexSearchQuery that = (ComplexSearchQuery) o;

        // Just check for set equality, order does not matter
        if (!(getDefaultFieldPhrases().containsAll(that.getDefaultFieldPhrases()) && that.getDefaultFieldPhrases().containsAll(getDefaultFieldPhrases()))) {
            return false;
        }
        if (!(getAuthors().containsAll(that.getAuthors()) && that.getAuthors().containsAll(getAuthors()))) {
            return false;
        }
        if (!(getTitlePhrases().containsAll(that.getTitlePhrases()) && that.getTitlePhrases().containsAll(getTitlePhrases()))) {
            return false;
        }
        if (getFromYear().isPresent() ? !getFromYear().equals(that.getFromYear()) : that.getFromYear().isPresent()) {
            return false;
        }
        if (getToYear().isPresent() ? !getToYear().equals(that.getToYear()) : that.getToYear().isPresent()) {
            return false;
        }
        if (getSingleYear().isPresent() ? !getSingleYear().equals(that.getSingleYear()) : that.getSingleYear().isPresent()) {
            return false;
        }
        return getJournal().isPresent() ? getJournal().equals(that.getJournal()) : !that.getJournal().isPresent();
    }

    @Override
    public int hashCode() {
        int result = defaultField != null ? defaultField.hashCode() : 0;
        result = 31 * result + (getAuthors() != null ? getAuthors().hashCode() : 0);
        result = 31 * result + (getTitlePhrases() != null ? getTitlePhrases().hashCode() : 0);
        result = 31 * result + (getFromYear().isPresent() ? getFromYear().hashCode() : 0);
        result = 31 * result + (getToYear().isPresent() ? getToYear().hashCode() : 0);
        result = 31 * result + (getSingleYear().isPresent() ? getSingleYear().hashCode() : 0);
        result = 31 * result + (getJournal().isPresent() ? getJournal().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder stringRepresentation = new StringBuilder();
        getSingleYear().ifPresent(singleYear -> stringRepresentation.append(singleYear).append(" "));
        getFromYear().ifPresent(fromYear -> stringRepresentation.append(fromYear).append(" "));
        getToYear().ifPresent(toYear -> stringRepresentation.append(toYear).append(" "));
        getJournal().ifPresent(journal -> stringRepresentation.append(journal).append(" "));
        stringRepresentation.append(String.join(" ", getTitlePhrases()))
                            .append(String.join(" ", getDefaultFieldPhrases()))
                            .append(String.join(" ", getAuthors()));
        return stringRepresentation.toString();
    }

    public static class ComplexSearchQueryBuilder {
        private List<String> defaultFieldPhrases = new ArrayList<>();
        private List<String> authors = new ArrayList<>();
        private List<String> titlePhrases = new ArrayList<>();
        private String journal;
        private Integer fromYear;
        private Integer toYear;
        private Integer singleYear;

        private ComplexSearchQueryBuilder() {
        }

        public ComplexSearchQueryBuilder defaultFieldPhrase(String defaultFieldPhrase) {
            if (Objects.requireNonNull(defaultFieldPhrase).isBlank()) {
                throw new IllegalArgumentException("Parameter must not be blank");
            }
            // Strip all quotes before wrapping
            this.defaultFieldPhrases.add(String.format("\"%s\"", defaultFieldPhrase.replace("\"", "")));
            return this;
        }

        /**
         * Adds author and wraps it in quotes
         */
        public ComplexSearchQueryBuilder author(String author) {
            if (Objects.requireNonNull(author).isBlank()) {
                throw new IllegalArgumentException("Parameter must not be blank");
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

        public ComplexSearchQueryBuilder terms(Collection<Term> terms) {
            terms.forEach(term -> {
                String termText = term.text();
                switch (term.field().toLowerCase()) {
                    case "author" -> this.author(termText);
                    case "title" -> this.titlePhrase(termText);
                    case "journal" -> this.journal(termText);
                    case "year" -> this.singleYear(Integer.valueOf(termText));
                    case "year-range" -> this.parseYearRange(termText);
                    case "default" -> this.defaultFieldPhrase(termText);
                }
            });
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
            return new ComplexSearchQuery(defaultFieldPhrases, authors, titlePhrases, fromYear, toYear, singleYear, journal);
        }

        void parseYearRange(String termText) {
            String[] split = termText.split("-");
            int fromYear = 0;
            int toYear = 9999;
            try {
                fromYear = Integer.parseInt(split[0]);
            } catch (NumberFormatException e) {
                // default value already set
            }
            if (split.length > 1) {
                try {
                    toYear = Integer.parseInt(split[1]);
                } catch (NumberFormatException e) {
                    // default value already set
                }
            }
            this.fromYearAndToYear(fromYear, toYear);
        }

        private boolean textSearchFieldsAndYearFieldsAreEmpty() {
            return this.stringListIsBlank(defaultFieldPhrases) && this.stringListIsBlank(titlePhrases) &&
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
