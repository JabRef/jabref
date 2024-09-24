package org.jabref.logic.citation.repository;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jabref.model.entry.BibEntry;

public class BibEntryRelationsRepositoryTestHelpers {
    public static class CreateRepository {
        public static BibEntryRelationsRepository from(
            Function<BibEntry, List<BibEntry>> retrieveCitations,
            Function<BibEntry, List<BibEntry>> retrieveReferences,
            Consumer<BibEntry> forceRefreshCitations,
            Consumer<BibEntry> forceRefreshReferences
        ) {
            return new BibEntryRelationsRepository() {
                @Override
                public List<BibEntry> readCitations(BibEntry entry) {
                    return retrieveCitations.apply(entry);
                }

                @Override
                public List<BibEntry> readReferences(BibEntry entry) {
                    return retrieveReferences.apply(entry);
                }

                @Override
                public void forceRefreshCitations(BibEntry entry) {
                    forceRefreshCitations.accept(entry);
                }

                @Override
                public void forceRefreshReferences(BibEntry entry) {
                    forceRefreshReferences.accept(entry);
                }
            };
        }
    }
}
