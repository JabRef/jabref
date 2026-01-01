package org.jabref.model.citation;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NonNull;

public record ReferenceEntry(
        @NonNull String rawText,
        @NonNull String marker,
        @NonNull Optional<String> authors,
        @NonNull Optional<String> title,
        @NonNull Optional<String> year,
        @NonNull Optional<String> journal,
        @NonNull Optional<String> volume,
        @NonNull Optional<String> pages,
        @NonNull Optional<String> doi,
        @NonNull Optional<String> url
) {
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern AUTHOR_YEAR_KEY_PATTERN = Pattern.compile(
            "([A-Z][a-z]+)(?:_|\\s*)?(\\d{4})"
    );

    public ReferenceEntry {
        if (rawText.isBlank()) {
            throw new IllegalArgumentException("Raw text cannot be blank");
        }
        if (marker.isBlank()) {
            throw new IllegalArgumentException("Marker cannot be blank");
        }
    }

    public ReferenceEntry(String rawText, String marker) {
        this(rawText, marker,
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    public ReferenceEntry(String rawText, String marker, String authors, String title, String year) {
        this(rawText, marker,
                Optional.ofNullable(authors).filter(s -> !s.isBlank()),
                Optional.ofNullable(title).filter(s -> !s.isBlank()),
                Optional.ofNullable(year).filter(s -> !s.isBlank()),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    public BibEntry toBibEntry() {
        StandardEntryType entryType = inferEntryType();
        BibEntry entry = new BibEntry(entryType);

        authors.ifPresent(a -> entry.setField(StandardField.AUTHOR, a));
        title.ifPresent(t -> entry.setField(StandardField.TITLE, t));
        year.ifPresent(y -> entry.setField(StandardField.YEAR, y));
        journal.ifPresent(j -> entry.setField(StandardField.JOURNAL, j));
        volume.ifPresent(v -> entry.setField(StandardField.VOLUME, v));
        pages.ifPresent(p -> entry.setField(StandardField.PAGES, p));
        doi.ifPresent(d -> entry.setField(StandardField.DOI, d));
        url.ifPresent(u -> entry.setField(StandardField.URL, u));

        generateCitationKey().ifPresent(entry::setCitationKey);

        return entry;
    }

    private StandardEntryType inferEntryType() {
        if (journal.isPresent()) {
            return StandardEntryType.Article;
        }
        if (url.isPresent() && title.isPresent() && journal.isEmpty()) {
            return StandardEntryType.Online;
        }
        return StandardEntryType.Misc;
    }

    public Optional<String> generateCitationKey() {
        if (authors.isEmpty() || year.isEmpty()) {
            return extractCitationKeyFromMarker();
        }

        String authorPart = extractFirstAuthorLastName(authors.get());
        String yearPart = year.get();

        if (authorPart.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(authorPart + yearPart);
    }

    private Optional<String> extractCitationKeyFromMarker() {
        String normalized = getNormalizedMarker();

        Matcher matcher = AUTHOR_YEAR_KEY_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return Optional.of(matcher.group(1) + matcher.group(2));
        }

        Matcher yearMatcher = YEAR_PATTERN.matcher(normalized);
        if (yearMatcher.find()) {
            String beforeYear = normalized.substring(0, yearMatcher.start()).trim();
            String[] words = beforeYear.split("\\s+");
            if (words.length > 0 && !words[0].isEmpty()) {
                String author = words[0].replaceAll("[^a-zA-Z]", "");
                if (!author.isEmpty()) {
                    return Optional.of(capitalize(author) + yearMatcher.group());
                }
            }
        }

        return Optional.empty();
    }

    private String extractFirstAuthorLastName(String authorString) {
        String cleaned = authorString
                .replaceAll("\\s+et\\s+al\\.?", "")
                .replaceAll("\\s+and\\s+.*", "")
                .replaceAll("\\s*,\\s*.*", "")
                .trim();

        String[] parts = cleaned.split("\\s+");
        if (parts.length == 0) {
            return "";
        }

        String lastName = parts[parts.length - 1];
        return capitalize(lastName.replaceAll("[^a-zA-Z]", ""));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public String getNormalizedMarker() {
        return marker
                .replaceAll("[\\[\\](){}]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String getSearchQuery() {
        if (doi.isPresent()) {
            return doi.get();
        }

        StringBuilder query = new StringBuilder();
        title.ifPresent(t -> query.append(t).append(" "));
        authors.ifPresent(a -> {
            String firstAuthor = extractFirstAuthorLastName(a);
            if (!firstAuthor.isEmpty()) {
                query.append(firstAuthor).append(" ");
            }
        });
        year.ifPresent(y -> query.append(y));

        return query.toString().trim();
    }

    public boolean hasMinimalMetadata() {
        return title.isPresent() || (authors.isPresent() && year.isPresent());
    }

    public boolean hasDoi() {
        return doi.isPresent();
    }

    public static Builder builder(String rawText, String marker) {
        return new Builder(rawText, marker);
    }

    public static class Builder {
        private final String rawText;
        private final String marker;
        private String authors;
        private String title;
        private String year;
        private String journal;
        private String volume;
        private String pages;
        private String doi;
        private String url;

        public Builder(String rawText, String marker) {
            this.rawText = rawText;
            this.marker = marker;
        }

        public Builder authors(String authors) {
            this.authors = authors;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder year(String year) {
            this.year = year;
            return this;
        }

        public Builder journal(String journal) {
            this.journal = journal;
            return this;
        }

        public Builder volume(String volume) {
            this.volume = volume;
            return this;
        }

        public Builder pages(String pages) {
            this.pages = pages;
            return this;
        }

        public Builder doi(String doi) {
            this.doi = doi;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public ReferenceEntry build() {
            return new ReferenceEntry(
                    rawText,
                    marker,
                    Optional.ofNullable(authors).filter(s -> !s.isBlank()),
                    Optional.ofNullable(title).filter(s -> !s.isBlank()),
                    Optional.ofNullable(year).filter(s -> !s.isBlank()),
                    Optional.ofNullable(journal).filter(s -> !s.isBlank()),
                    Optional.ofNullable(volume).filter(s -> !s.isBlank()),
                    Optional.ofNullable(pages).filter(s -> !s.isBlank()),
                    Optional.ofNullable(doi).filter(s -> !s.isBlank()),
                    Optional.ofNullable(url).filter(s -> !s.isBlank())
            );
        }
    }
}
