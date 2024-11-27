package org.jabref.logic.citation.repository;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jabref.model.entry.BibEntry;

public class BibEntryRelationsRepositoryHelpersForTest {
    public static class Mocks {
        public static BibEntryRelationsRepository from(
            Function<BibEntry, List<BibEntry>> retrieveCitations,
            BiConsumer<BibEntry, List<BibEntry>> insertCitations,
            Function<BibEntry, List<BibEntry>> retrieveReferences,
            BiConsumer<BibEntry, List<BibEntry>> insertReferences
        ) {
            return new BibEntryRelationsRepository() {
                @Override
                public void insertCitations(BibEntry entry, List<BibEntry> citations) {
                    insertCitations.accept(entry, citations);
                }

                @Override
                public List<BibEntry> readCitations(BibEntry entry) {
                    return retrieveCitations.apply(entry);
                }

                @Override
                public boolean containsCitations(BibEntry entry) {
                    return true;
                }

                @Override
                public boolean isCitationsUpdatable(BibEntry entry) {
                    return true;
                }

                @Override
                public void insertReferences(BibEntry entry, List<BibEntry> citations) {
                    insertReferences.accept(entry, citations);
                }

                @Override
                public List<BibEntry> readReferences(BibEntry entry) {
                    return retrieveReferences.apply(entry);
                }

                @Override
                public boolean containsReferences(BibEntry entry) {
                    return true;
                }

                @Override
                public boolean isReferencesUpdatable(BibEntry entry) {
                    return true;
                }
            };
        }

        public static BibEntryRelationsRepository from(
            Map<BibEntry, List<BibEntry>> citationsDB, Map<BibEntry, List<BibEntry>> referencesDB
        ) {
            return new BibEntryRelationsRepository() {
                @Override
                public void insertCitations(BibEntry entry, List<BibEntry> citations) {
                    citationsDB.put(entry, citations);
                }

                @Override
                public List<BibEntry> readCitations(BibEntry entry) {
                    return citationsDB.getOrDefault(entry, List.of());
                }

                @Override
                public boolean containsCitations(BibEntry entry) {
                    return citationsDB.containsKey(entry);
                }

                @Override
                public boolean isCitationsUpdatable(BibEntry entry) {
                    return true;
                }

                @Override
                public void insertReferences(BibEntry entry, List<BibEntry> citations) {
                    referencesDB.put(entry, citations);
                }

                @Override
                public List<BibEntry> readReferences(BibEntry entry) {
                    return referencesDB.getOrDefault(entry, List.of());
                }

                @Override
                public boolean containsReferences(BibEntry entry) {
                    return referencesDB.containsKey(entry);
                }

                @Override
                public boolean isReferencesUpdatable(BibEntry entry) {
                    return true;
                }
            };
        }
    }
}
