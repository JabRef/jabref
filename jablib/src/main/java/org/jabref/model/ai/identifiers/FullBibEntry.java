package org.jabref.model.ai.identifiers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.ai.summarization.AiSummaryIdentifier;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public record FullBibEntry(BibDatabaseContext databaseContext, BibEntry entry) {
    /// Creates an {@link AiSummaryIdentifier} for this entry if it has both a valid AI library ID
    /// and a present, unique citation key.
    ///
    /// @return {@link Optional#empty()} if the preconditions are not met
    public Optional<AiSummaryIdentifier> toAiSummaryIdentifier() {
        return AiSummaryIdentifier.from(databaseContext, entry);
    }

    public static Stream<FullBibEntry> fromSeveral(BibDatabaseContext databaseContext, Stream<BibEntry> entries) {
        return entries.map(entry -> new FullBibEntry(databaseContext, entry));
    }

    public static List<FullBibEntry> fromSeveral(BibDatabaseContext databaseContext, List<BibEntry> entries) {
        return fromSeveral(databaseContext, entries.stream()).toList();
    }

    public static Optional<BibEntry> findEntryByLink(List<FullBibEntry> entries, String link) {
        return entries.stream()
                      .flatMap(identifier -> identifier.databaseContext().getEntries().stream())
                      .filter(entry -> entry.getFiles().stream().anyMatch(file -> file.getLink().equals(link)))
                      .findFirst();
    }

    public static Optional<BibEntry> findEntryByLink(FullBibEntry entry, String link) {
        return findEntryByLink(List.of(entry), link);
    }
}
