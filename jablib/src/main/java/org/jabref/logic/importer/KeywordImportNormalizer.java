package org.jabref.logic.importer;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeKeywordDelimitersFormatter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class KeywordImportNormalizer {

    private static final List<Character> DEFAULT_IMPORT_KEYWORD_DELIMITERS = List.of(';', ',');

    private KeywordImportNormalizer() {
    }

    /// [impl->req~import.bibtex.keywords.normalize-delimiters~1]
    public static void normalizeKeywords(Iterable<BibEntry> entries, BibEntryPreferences preferences) {
        entries.forEach(entry -> normalizeKeywords(entry, preferences));
    }

    public static void normalizeKeywords(Iterable<BibEntry> entries, BibEntryPreferences preferences, @Nullable Character separator) {
        entries.forEach(entry -> normalizeKeywords(entry, preferences, separator));
    }

    public static void normalizeKeywords(BibEntry entry, BibEntryPreferences preferences) {
        normalizeKeywords(entry, preferences, preferences.getKeywordSeparator());
    }

    /// Guesses the separator the given entries already use: the configured import delimiter occurring most often in keyword fields.
    /// Only effective delimiters count (top level, not escaped, not inside braces), matching how [KeywordList] parses.
    /// Empty if no keyword field contains any delimiter, or if two delimiters are tied (then the caller's fallback applies).
    public static Optional<Character> guessSeparator(Iterable<BibEntry> entries, BibEntryPreferences preferences) {
        List<Character> candidates = parseConfiguredDelimiters(preferences.getImportKeywordDelimiters());
        Map<Character, Long> counts = new HashMap<>();
        for (BibEntry entry : entries) {
            entry.getField(StandardField.KEYWORDS).ifPresent(keywords -> {
                for (Character candidate : candidates) {
                    long occurrences = KeywordList.countEffectiveDelimiters(keywords, candidate);
                    if (occurrences > 0) {
                        counts.merge(candidate, occurrences, Long::sum);
                    }
                }
            });
        }
        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<Character> winners = counts.entrySet().stream().filter(count -> count.getValue() == max).map(Map.Entry::getKey).toList();
        return (max > 0) && (winners.size() == 1) ? Optional.of(winners.getFirst()) : Optional.empty();
    }

    /// Applies [NormalizeKeywordDelimitersFormatter] to the entry, i.e. the same cleanup that is available as a save action.
    public static void normalizeKeywords(BibEntry entry, BibEntryPreferences preferences, @Nullable Character requestedSeparator) {
        Formatter formatter = new NormalizeKeywordDelimitersFormatter(preferences, requestedSeparator);
        new FieldFormatterCleanup(StandardField.KEYWORDS, formatter).cleanup(entry);
    }

    public static List<Character> parseConfiguredDelimiters(@Nullable String configuredDelimiters) {
        return Optional.ofNullable(configuredDelimiters)
                       .filter(delimiters -> !delimiters.isBlank())
                       .map(KeywordImportNormalizer::splitDelimiters)
                       .filter(delimiters -> !delimiters.isEmpty())
                       .orElse(DEFAULT_IMPORT_KEYWORD_DELIMITERS);
    }

    private static List<Character> splitDelimiters(@NonNull String configuredDelimiters) {
        SequencedSet<Character> delimiters = new LinkedHashSet<>();
        configuredDelimiters.chars()
                            .mapToObj(symbol -> (char) symbol)
                            .filter(symbol -> !Character.isWhitespace(symbol))
                            .filter(symbol -> symbol != Keyword.DEFAULT_HIERARCHICAL_DELIMITER)
                            .filter(symbol -> symbol != Keyword.DEFAULT_ESCAPE_SYMBOL)
                            .forEach(delimiters::add);
        return List.copyOf(delimiters);
    }
}
