package org.jabref.logic.importer.relatedwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable metrics summary for one evaluation run.
 */
public final class RelatedWorkMetrics {

    /**
     * Per-entry result summary.
     */
    public static final class PerEntry {
        public final String key; // canonical key: firstAuthor-lastname-lower + "-" + year
        public final int expectedSnippets;
        public final int extractedSnippets;
        public final int truePositiveSnippets;

        public PerEntry(String key,
                        int expectedSnippets,
                        int extractedSnippets,
                        int truePositiveSnippets) {
            this.key = key;
            this.expectedSnippets = expectedSnippets;
            this.extractedSnippets = extractedSnippets;
            this.truePositiveSnippets = truePositiveSnippets;
        }
    }

    public final int expectedPairs;
    public final int extractedPairs;
    public final int truePositives;
    public final int falsePositives;
    public final int falseNegatives;

    public final double precision;
    public final double recall;
    public final double f1;

    public final int distinctEntriesExpected;
    public final int distinctEntriesAnnotated;
    public final double avgSnippetsPerAnnotatedEntry;

    public final List<PerEntry> perEntry;

    public RelatedWorkMetrics(int expectedPairs,
                              int extractedPairs,
                              int truePositives,
                              int falsePositives,
                              int falseNegatives,
                              int distinctEntriesExpected,
                              int distinctEntriesAnnotated,
                              double avgSnippetsPerAnnotatedEntry,
                              List<PerEntry> perEntry) {

        this.expectedPairs = expectedPairs;
        this.extractedPairs = extractedPairs;
        this.truePositives = truePositives;
        this.falsePositives = falsePositives;
        this.falseNegatives = falseNegatives;
        this.distinctEntriesExpected = distinctEntriesExpected;
        this.distinctEntriesAnnotated = distinctEntriesAnnotated;
        this.avgSnippetsPerAnnotatedEntry = avgSnippetsPerAnnotatedEntry;

        this.precision = (truePositives + falsePositives) == 0
                         ? 0.0
                         : (double) truePositives / (truePositives + falsePositives);
        this.recall = (truePositives + falseNegatives) == 0
                      ? 0.0
                      : (double) truePositives / (truePositives + falseNegatives);
        this.f1 = (precision + recall) == 0
                  ? 0.0
                  : (2.0 * precision * recall) / (precision + recall);

        this.perEntry = Collections.unmodifiableList(new ArrayList<>(perEntry));
    }

    public String pretty() {
        double coverage = distinctEntriesExpected == 0
                          ? 0.0
                          : 100.0 * distinctEntriesAnnotated / distinctEntriesExpected;
        return String.format(Locale.ROOT,
                "RelatedWork Metrics:%n"
                        + "  Pairs  — expected=%d, extracted=%d, TP=%d, FP=%d, FN=%d%n"
                        + "  Scores — precision=%.3f, recall=%.3f, F1=%.3f%n"
                        + "  Coverage — entries_expected=%d, entries_annotated=%d (%.1f%%), "
                        + "avg_snippets/annotated_entry=%.2f",
                expectedPairs, extractedPairs, truePositives, falsePositives, falseNegatives,
                precision, recall, f1,
                distinctEntriesExpected, distinctEntriesAnnotated, coverage,
                avgSnippetsPerAnnotatedEntry);
    }

    /**
     * Builds per-entry summaries from confusion sets.
     */
    static List<PerEntry> perEntryFrom(Map<String, List<String>> expectedByKey,
                                       Map<String, List<String>> extractedByKey,
                                       Map<String, Set<Integer>> matchedIdxByKey) {

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(expectedByKey.keySet());
        allKeys.addAll(extractedByKey.keySet());

        return allKeys.stream()
                      .sorted()
                      .map(k -> new PerEntry(
                              k,
                              expectedByKey.getOrDefault(k, List.of()).size(),
                              extractedByKey.getOrDefault(k, List.of()).size(),
                              matchedIdxByKey.getOrDefault(k, Set.of()).size()))
                      .collect(Collectors.toList());
    }
}
