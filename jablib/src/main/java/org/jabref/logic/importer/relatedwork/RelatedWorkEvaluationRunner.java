package org.jabref.logic.importer.relatedwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * Deterministic evaluator for citation-context extraction.
 * Compares extractor output against a gold fixture and computes metrics.
 */
public final class RelatedWorkEvaluationRunner {

    /**
     * Minimal adapter: sectionText + candidate entries -> mapping(entryKey -> list of snippets).
     */
    @FunctionalInterface
    public interface Extractor extends BiFunction<String, List<BibEntry>, Map<BibEntry, List<String>>> {
    }

    private final Extractor extractor;

    public RelatedWorkEvaluationRunner(Extractor extractor) {
        this.extractor = Objects.requireNonNull(extractor);
    }

    /**
     * Canonical key = firstAuthorSurnameLower + "-" + year (missing parts become "unknown").
     */
    public static String canonicalKey(BibEntry entry) {
        String author = entry.getField(StandardField.AUTHOR).orElse("").trim();
        String year = entry.getField(StandardField.YEAR).orElse("").trim();
        String last = firstAuthorSurname(author);
        if (last.isBlank()) {
            last = "unknown";
        }
        if (year.isBlank()) {
            year = "unknown";
        }
        return last.toLowerCase(Locale.ROOT) + "-" + year;
    }

    /**
     * Very small surname parser: takes first “word” before comma or the last token.
     */
    static String firstAuthorSurname(String authorField) {
        if (authorField == null || authorField.isBlank()) {
            return "";
        }
        // Examples: "Vesce, E.; Olivieri, G.; ..." or "Bianchi, F. R." or "Luisa Marcela Luna Ostos and ..."
        String primary = authorField.split("(?i)\\band\\b|;")[0].trim();
        if (primary.contains(",")) {
            return primary.substring(0, primary.indexOf(',')).trim();
        }
        String[] tokens = primary.trim().split("\\s+");
        return tokens.length == 0 ? "" : tokens[tokens.length - 1];
    }

    /**
     * Runs one evaluation against a single fixture.
     */
    public RelatedWorkMetrics run(RelatedWorkFixture fixture, List<BibEntry> candidates) {
        Map<BibEntry, List<String>> extractedRaw = extractor.apply(fixture.relatedWorkText, candidates);

        // Canonicalize extractor output -> key -> snippets
        Map<String, List<String>> extractedByKey = new HashMap<>();
        extractedRaw.forEach((entry, snippets) -> {
            String key = canonicalKey(entry);
            extractedByKey.computeIfAbsent(key, k -> new ArrayList<>())
                          .addAll(normalizeSnippets(snippets));
        });

        // Expected: already in canonical key space
        Map<String, List<String>> expectedByKey = new HashMap<>();
        for (RelatedWorkFixture.Expectation exp : fixture.expectations) {
            expectedByKey.computeIfAbsent(exp.canonicalKey(), k -> new ArrayList<>())
                         .add(normalize(exp.snippetContains));
        }

        // For matching snippets we do "expected substring contained in extracted"
        // (case-insensitive, simple fuzz). Track matched extracted indices per key.
        int truePositives = 0;
        Map<String, Set<Integer>> matchedExtractedIdxByKey = new HashMap<>();

        for (Map.Entry<String, List<String>> kv : expectedByKey.entrySet()) {
            String key = kv.getKey();
            List<String> expectedSnippets = kv.getValue();
            List<String> extractedSnippets = extractedByKey.getOrDefault(key, List.of());

            boolean[] taken = new boolean[extractedSnippets.size()];
            for (String expNeedle : expectedSnippets) {
                int matchIdx = indexOfContaining(extractedSnippets, expNeedle, taken);
                if (matchIdx >= 0) {
                    taken[matchIdx] = true;
                    matchedExtractedIdxByKey
                            .computeIfAbsent(key, k -> new HashSet<>())
                            .add(matchIdx);
                    truePositives++;
                }
            }
        }

        int extractedPairs = extractedByKey.values().stream().mapToInt(List::size).sum();
        int expectedPairs = expectedByKey.values().stream().mapToInt(List::size).sum();
        int falsePositives = extractedPairs - truePositives;
        int falseNegatives = expectedPairs - truePositives;

        int distinctExpected = expectedByKey.size();
        int distinctAnnotated = (int) extractedByKey.entrySet()
                                                    .stream()
                                                    .filter(e -> !e.getValue().isEmpty())
                                                    .count();
        double avgSnippetsPerAnnotated =
                distinctAnnotated == 0 ? 0.0 : (double) extractedPairs / distinctAnnotated;

        return new RelatedWorkMetrics(
                expectedPairs,
                extractedPairs,
                truePositives,
                falsePositives,
                falseNegatives,
                distinctExpected,
                distinctAnnotated,
                avgSnippetsPerAnnotated,
                RelatedWorkMetrics.perEntryFrom(expectedByKey,
                        extractedByKey,
                        matchedExtractedIdxByKey)
        );
    }

    // ---------- helpers ----------

    private static List<String> normalizeSnippets(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                  .map(RelatedWorkEvaluationRunner::normalize)
                  .collect(Collectors.toList());
    }

    /**
     * Lowercases, collapses whitespace, strips trailing punctuation for robust substring checks.
     */
    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String t = s.toLowerCase(Locale.ROOT);
        t = t.replaceAll("\\s+", " ").trim();
        t = t.replaceAll("[\\p{Punct}]+$", "");
        return t;
    }

    /**
     * Returns first index where haystack[i] contains needle (both normalized),
     * honoring "taken" to avoid double-counting.
     */
    static int indexOfContaining(List<String> haystack, String needle, boolean[] taken) {
        for (int i = 0; i < haystack.size(); i++) {
            if (taken[i]) {
                continue;
            }
            String h = haystack.get(i);
            if (h.contains(needle)) {
                return i;
            }
        }
        return -1;
    }
}
