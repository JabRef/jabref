package org.jabref.logic.importer.fileformat.pdf;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISBN;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class BookIsbnResolver {

    static final int MAX_CANDIDATES = 5;
    private static final Pattern SEPARATORS = Pattern.compile("[\\p{P}\\p{Z}\\s]+");
    private static final Pattern YEAR = Pattern.compile("[0-9]{4}");

    private enum Assessment {
        MATCH,
        MISMATCH,
        UNKNOWN
    }

    boolean hasRequiredMetadata(BibEntry localBook) {
        return List.of(StandardField.TITLE, contributorField(localBook), StandardField.PUBLISHER, StandardField.YEAR)
                   .stream()
                   .allMatch(field -> normalizedField(localBook, field).isPresent());
    }

    Optional<ISBN> resolve(BibEntry localBook, Map<ISBN, Optional<BibEntry>> candidates) {
        if (!hasRequiredMetadata(localBook) || candidates.size() < 2 || candidates.size() > MAX_CANDIDATES) {
            return Optional.empty();
        }

        Optional<ISBN> selectedIsbn = Optional.empty();
        for (Map.Entry<ISBN, Optional<BibEntry>> candidate : candidates.entrySet()) {
            if (candidate.getValue().isEmpty()) {
                return Optional.empty();
            }

            BibEntry fetchedEntry = candidate.getValue().orElseThrow();
            Optional<ISBN> returnedIsbn = fetchedEntry.getField(StandardField.ISBN)
                                                     .flatMap(value -> ISBN.findInText("ISBN " + value));
            if (!returnedIsbn.equals(Optional.of(candidate.getKey()))) {
                return Optional.empty();
            }

            Assessment assessment = assess(localBook, fetchedEntry);
            if (assessment == Assessment.UNKNOWN) {
                return Optional.empty();
            }
            if (assessment == Assessment.MATCH) {
                if (selectedIsbn.isPresent()) {
                    return Optional.empty();
                }
                selectedIsbn = Optional.of(candidate.getKey());
            }
        }
        return selectedIsbn;
    }

    private Assessment assess(BibEntry localBook, BibEntry candidate) {
        Assessment title = compareField(localBook, candidate, StandardField.TITLE);
        Assessment contributors = compareField(localBook, candidate, contributorField(localBook));
        Assessment publisher = compareField(localBook, candidate, StandardField.PUBLISHER);
        Assessment year = compareField(localBook, candidate, StandardField.YEAR);
        List<Assessment> evidence = List.of(title, contributors, publisher, year);

        if (evidence.contains(Assessment.UNKNOWN)) {
            return Assessment.UNKNOWN;
        }
        if (localBook.getField(StandardField.EDITION).isPresent()
                && compareField(localBook, candidate, StandardField.EDITION) != Assessment.MATCH) {
            return Assessment.UNKNOWN;
        }

        if (title == Assessment.MATCH && contributors == Assessment.MATCH) {
            if (year == Assessment.MISMATCH) {
                return Assessment.MISMATCH;
            }
            if (publisher == Assessment.MATCH) {
                return Assessment.MATCH;
            }
        }
        if (evidence.stream().allMatch(value -> value == Assessment.MISMATCH)) {
            return Assessment.MISMATCH;
        }
        return Assessment.UNKNOWN;
    }

    private StandardField contributorField(BibEntry localBook) {
        if (localBook.getField(StandardField.AUTHOR).isPresent()) {
            return StandardField.AUTHOR;
        }
        return StandardField.EDITOR;
    }

    private Assessment compareField(BibEntry localBook, BibEntry candidate, StandardField field) {
        Optional<String> localValue = normalizedField(localBook, field);
        Optional<String> fetchedValue = normalizedField(candidate, field);
        if (localValue.isEmpty() || fetchedValue.isEmpty()) {
            return Assessment.UNKNOWN;
        }
        if (localValue.equals(fetchedValue)) {
            return Assessment.MATCH;
        }
        return Assessment.MISMATCH;
    }

    private Optional<String> normalizedField(BibEntry entry, StandardField field) {
        return entry.getField(field)
                    .filter(value -> field != StandardField.YEAR || YEAR.matcher(value.strip()).matches())
                    .map(value -> normalizeField(field, value))
                    .filter(value -> !value.isBlank());
    }

    private String normalizeField(StandardField field, String value) {
        String text = value;
        if (field == StandardField.AUTHOR || field == StandardField.EDITOR) {
            text = AuthorList.parse(value).getAsLastFirstNamesWithAnd(true);
        }
        text = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        return SEPARATORS.matcher(text).replaceAll(" ").strip();
    }
}
