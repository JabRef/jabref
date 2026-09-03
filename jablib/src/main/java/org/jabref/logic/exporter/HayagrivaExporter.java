package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import tools.jackson.core.JacksonException;

import static java.util.function.Predicate.not;

/// Exports entries as one Hayagriva YAML document (a map of citation key to entry), delegating
/// the field mapping to [HayagrivaEntryWriter] so that exports read back losslessly through the
/// Hayagriva importer.
@NullMarked
public class HayagrivaExporter extends Exporter {

    private final HayagrivaEntryWriter entryWriter = new HayagrivaEntryWriter();

    public HayagrivaExporter() {
        super("hayagrivayaml", "Hayagriva YAML", StandardFileType.YAML);
    }

    @Override
    public void export(@NonNull BibDatabaseContext databaseContext, Path file, @NonNull List<BibEntry> entries) throws IOException {
        if (entries.isEmpty()) {
            return;
        }
        try {
            Files.writeString(file, entryWriter.serialize(keyedUniquely(entries)), StandardCharsets.UTF_8);
        } catch (JacksonException e) {
            throw new IOException("Could not serialize entries to Hayagriva YAML", e);
        }
    }

    /// The top-level YAML keys are the citation keys, which YAML requires to be unique within
    /// the document; duplicates and missing keys get a positional fallback.
    private SequencedMap<String, BibEntry> keyedUniquely(List<BibEntry> entries) {
        SequencedMap<String, BibEntry> keyedEntries = new LinkedHashMap<>();
        int position = 0;
        for (BibEntry entry : entries) {
            position++;
            String key = entry.getCitationKey()
                              .map(String::trim)
                              .filter(not(String::isEmpty))
                              .orElse("entry-" + position);
            String uniqueKey = key;
            int suffix = 1;
            while (keyedEntries.containsKey(uniqueKey)) {
                uniqueKey = key + "-" + suffix++;
            }
            keyedEntries.put(uniqueKey, entry);
        }
        return keyedEntries;
    }
}
