package org.jabref.logic.citation.repository;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jabref.model.entry.BibEntry;

/**
 * Provide helpers methods and classes for tests to manage {@link BibEntryCitationsAndReferencesRepository} mocks.
 */
public class BibEntryRelationsRepositoryTestHelpers {

    /**
     * Provide mocks factories for {@link BibEntryCitationsAndReferencesRepository} mocks.
     * <br>
     * Those implementations should help to test the values passed to an injected repository instance
     * when it is called from {@link org.jabref.logic.citation.SearchCitationsRelationsService}.
     */
    public static class Mocks {
        public static BibEntryCitationsAndReferencesRepository from(
                Function<BibEntry, List<BibEntry>> retrieveCitations,
                BiConsumer<BibEntry, List<BibEntry>> insertCitations,
                Function<BibEntry, List<BibEntry>> retrieveReferences,
                BiConsumer<BibEntry, List<BibEntry>> insertReferences,
                Function<BibEntry, Boolean> isCitationsUpdatable,
                Function<BibEntry, Boolean> isReferencesUpdatable
        ) {
            return new BibEntryCitationsAndReferencesRepository() {
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
                    return isCitationsUpdatable.apply(entry);
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
                    return isReferencesUpdatable.apply(entry);
                }

                @Override
                public void close() {
                    // Nothing to do
                }
            };
        }

        public static BibEntryCitationsAndReferencesRepository from(
                Map<BibEntry, List<BibEntry>> citationsDB, Map<BibEntry, List<BibEntry>> referencesDB, boolean isCitationsUpdatable
        ) {
            return new BibEntryCitationsAndReferencesRepository() {
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
                    return isCitationsUpdatable;
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

                @Override
                public void close() {
                    // Nothing to do
                }
            };
        }
    }
}
