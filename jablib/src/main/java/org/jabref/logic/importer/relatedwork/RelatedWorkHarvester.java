package org.jabref.logic.importer.relatedwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.jabref.logic.importer.RelatedWorkAnnotator;
import org.jabref.model.entry.BibEntry;

/**
 * Orchestrates extraction and annotation from "Related Work" text.
 * Backward-compatible: existing APIs preserved.
 * Optionally uses plugin SPI (summarizer / resolver) when provided via config.
 */
public class RelatedWorkHarvester {

    private final RelatedWorkExtractor extractor;
    private final RelatedWorkPluginConfig plugins; // may be defaults (no-ops)

    /**
     * Backward-compatible constructor: no plugins, identical behavior to previous version.
     */
    public RelatedWorkHarvester(RelatedWorkExtractor extractor) {
        this(extractor, RelatedWorkPluginConfig.builder().build());
    }

    /**
     * New constructor that accepts plugin config (summarization / resolution).
     */
    public RelatedWorkHarvester(RelatedWorkExtractor extractor, RelatedWorkPluginConfig plugins) {
        this.extractor = Objects.requireNonNull(extractor);
        this.plugins = (plugins == null) ? RelatedWorkPluginConfig.builder().build() : plugins;
    }

    /**
     * Existing API
     */
    public void harvestAndAnnotate(
            String username,
            String citingPaperKey,
            String fullText,
            List<BibEntry> bibliography,
            Consumer<BibEntry> addOrUpdateFn
    ) {
        annotateInternal(username, citingPaperKey, fullText, bibliography, addOrUpdateFn);
    }

    /**
     * Test-friendly: returns number of entries annotated.
     */
    public int harvestAndAnnotateCount(
            String username,
            String citingPaperKey,
            String fullText,
            List<BibEntry> bibliography,
            Consumer<BibEntry> addOrUpdateFn
    ) {
        return annotateInternal(username, citingPaperKey, fullText, bibliography, addOrUpdateFn);
    }

    private int annotateInternal(
            String username,
            String citingPaperKey,
            String fullText,
            List<BibEntry> bibliography,
            Consumer<BibEntry> addOrUpdateFn
    ) {
        // Extract citationKey -> snippet (heuristic extractor)
        Map<String, String> summariesByKey = extractor.extract(fullText, bibliography);

        // Build quick index for existing entries by citation key
        Map<String, BibEntry> entryByKey = new HashMap<>();
        for (BibEntry be : bibliography) {
            be.getCitationKey().ifPresent(k -> entryByKey.put(k, be));
        }

        // Group snippets per BibEntry (supports future extractors that may yield >1 per entry)
        Map<BibEntry, List<String>> snippetsByEntry = new HashMap<>();

        for (Map.Entry<String, String> e : summariesByKey.entrySet()) {
            String citedKey = e.getKey();
            String snippet = e.getValue();

            // Try direct match
            BibEntry entry = entryByKey.get(citedKey);

            // Optional: resolver plug-in to handle missing keys
            if (entry == null && plugins.isResolutionEnabled()) {
                Optional<BibEntry> resolved = plugins.resolver().resolveByKey(citedKey, bibliography);
                if (resolved.isEmpty()) {
                    // Heuristic fallback: infer (surname, year) from key like "Vesce2016"
                    String lower = citedKey.toLowerCase(Locale.ROOT);
                    String year = onlyDigits(lower);
                    String surname = stripDigits(lower);
                    if (!surname.isEmpty() && year.length() == 4) {
                        resolved = plugins.resolver().resolveByAuthorYear(surname, year, bibliography);
                    }
                }
                if (resolved.isEmpty()) {
                    // Optional: allow creation of a shell entry
                    resolved = plugins.resolver().createIfMissing(citedKey);
                    resolved.ifPresent(bibliography::add);
                }
                if (resolved.isPresent()) {
                    entry = resolved.get();
                    // capture entry in an effectively-final local before using it in a lambda
                    final BibEntry entryForMap = entry;
                    entry.getCitationKey().ifPresent(k -> entryByKey.put(k, entryForMap));
                }
            }

            if (entry == null) {
                // Preserve previous behavior: create a new entry if none found (no plugin)
                entry = findOrCreateEntry(citedKey, bibliography, addOrUpdateFn);
                entryByKey.put(citedKey, entry);
            }

            // Make the captured variable effectively final for the lambda below
            final BibEntry target = entry;
            snippetsByEntry.computeIfAbsent(target, k -> new ArrayList<>()).add(snippet);
        }

        // Summarize (optional) or append individual snippets (default)
        int updated = 0;
        for (Map.Entry<BibEntry, List<String>> kv : snippetsByEntry.entrySet()) {
            BibEntry entry = kv.getKey();
            List<String> snippets = kv.getValue();

            if (plugins.isSummarizationEnabled()) {
                Optional<String> summary = plugins.summarizer().summarize(snippets, entry, 300);
                if (summary.isPresent()) {
                    RelatedWorkAnnotator.appendSummaryToEntry(entry, username, citingPaperKey, summary.get());
                    addOrUpdateFn.accept(entry);
                    updated++;
                    continue;
                }
            }

            // Default: append each snippet individually
            for (String s : snippets) {
                RelatedWorkAnnotator.appendSummaryToEntry(entry, username, citingPaperKey, s);
                addOrUpdateFn.accept(entry);
                updated++;
            }
        }
        return updated;
    }

    private static String onlyDigits(String s) {
        return s.replaceAll("[^0-9]", "");
    }

    private static String stripDigits(String s) {
        return s.replaceAll("[0-9]", "");
    }

    private BibEntry findOrCreateEntry(String key, List<BibEntry> bibs, Consumer<BibEntry> addOrUpdateFn) {
        Optional<BibEntry> found = bibs.stream()
                                       .filter(b -> b.getCitationKey().orElse("").equals(key))
                                       .findFirst();

        if (found.isPresent()) {
            return found.get();
        }

        BibEntry newEntry = new BibEntry();
        newEntry.setCitationKey(key);
        addOrUpdateFn.accept(newEntry);
        return newEntry;
    }
}
