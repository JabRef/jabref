package org.jabref.logic.importer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;

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

    public static void normalizeKeywords(BibEntry entry, BibEntryPreferences preferences) {
        Character separator = Optional.ofNullable(preferences.getKeywordSeparator())
                                      .orElse(BibEntryPreferences.getDefault().getKeywordSeparator());
        List<Character> importKeywordDelimiters = parseConfiguredDelimiters(preferences.getImportKeywordDelimiters());
        BibEntryPreferences.ImportDelimiterParsingStrategy parsingStrategy = Optional.ofNullable(preferences.getImportDelimiterParsingStrategy())
                                                                                     .orElse(BibEntryPreferences.ImportDelimiterParsingStrategy.SPLIT_ON_ALL_DELIMITERS);

        entry.getField(StandardField.KEYWORDS).ifPresent(rawKeywords -> {
            KeywordList importedKeywords = switch (parsingStrategy) {
                case SPLIT_ON_ALL_DELIMITERS ->
                        KeywordList.parseWithMultipleDelimiters(rawKeywords, importKeywordDelimiters);
                case INFER_DELIMITER_BY_PRIORITY ->
                        KeywordList.parseWithPrioritizedDelimiters(rawKeywords, importKeywordDelimiters);
            };
            entry.setField(StandardField.KEYWORDS, KeywordList.serializeWithSpaces(importedKeywords.stream().toList(), separator));
        });
    }

    static List<Character> parseConfiguredDelimiters(@Nullable String configuredDelimiters) {
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
