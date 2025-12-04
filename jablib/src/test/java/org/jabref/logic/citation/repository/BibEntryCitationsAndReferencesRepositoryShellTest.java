package org.jabref.logic.citation.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.random.RandomGenerator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BibEntryCitationsAndReferencesRepositoryShellTest {

    private static BibEntry createBibEntry() {
        int i = RandomGenerator.getDefault().nextInt();
        return new BibEntry()
                .withCitationKey(String.valueOf(i))
                .withField(StandardField.DOI, "10.1234/5678" + i);
    }

    private static List<BibEntry> createRelations(BibEntry entry) {
        return entry
                .getCitationKey()
                .map(key -> RandomGenerator
                        .StreamableGenerator.of("L128X256MixRandom").ints(10)
                                            .mapToObj(i -> new BibEntry()
                                                    .withCitationKey("%s relation %s".formatted(key, i))
                                                    .withField(StandardField.DOI, "10.2345/6789" + i)
                                            )
                )
                .orElseThrow()
                .toList();
    }

    private static class BibEntryRelationRepositoryMock implements BibEntryRelationRepository {

        private final HashMap<BibEntry, List<BibEntry>> relations = new HashMap<>();
        private boolean closeCalled = false;
        private final boolean isUpdatable;

        public BibEntryRelationRepositoryMock(boolean isUpdatable) {
            this.isUpdatable = isUpdatable;
        }

        public BibEntryRelationRepositoryMock() {
            this(false);
        }

        @Override
        public List<BibEntry> getRelations(BibEntry entry) {
            return new ArrayList<>(this.relations.getOrDefault(entry, List.of()));
        }

        @Override
        public void addRelations(BibEntry entry, List<BibEntry> relations) {
            this.relations.put(entry, relations);
        }

        @Override
        public boolean containsKey(BibEntry entry) {
            return this.relations.containsKey(entry);
        }

        @Override
        public boolean shouldUpdate(BibEntry entry) {
            return this.isUpdatable;
        }

        @Override
        public void close() {
            this.closeCalled = true;
        }

        public boolean wasClosed() {
            return this.closeCalled;
        }
    }

    @Test
    void repositoryShouldWriteAndReadCitationsToAndFromExpectedDAO() {
        // GIVEN
        BibEntry bibEntry = createBibEntry();
        List<BibEntry> citations = createRelations(bibEntry);
        BibEntryRelationRepositoryMock citationsDAO = new BibEntryRelationRepositoryMock();
        BibEntryCitationsAndReferencesRepositoryShell bibEntryRelationsRepository = new BibEntryCitationsAndReferencesRepositoryShell(
                citationsDAO,
                new BibEntryRelationRepositoryMock()
        );
        Assertions.assertFalse(bibEntryRelationsRepository.containsCitations(bibEntry));
        Assertions.assertFalse(citations.isEmpty());

        // WHEN
        bibEntryRelationsRepository.insertCitations(bibEntry, citations);

        // THEN
        Assertions.assertTrue(bibEntryRelationsRepository.containsCitations(bibEntry));
        Assertions.assertEquals(citations, bibEntryRelationsRepository.readCitations(bibEntry));
    }

    @Test
    void repositoryShouldWriteAndReadReferencesToAndFromExpectedDAO() {
        // GIVEN
        BibEntry bibEntry = createBibEntry();
        List<BibEntry> references = createRelations(bibEntry);
        BibEntryRelationRepositoryMock referencesDAO = new BibEntryRelationRepositoryMock();
        BibEntryCitationsAndReferencesRepositoryShell bibEntryRelationsRepository = new BibEntryCitationsAndReferencesRepositoryShell(
                new BibEntryRelationRepositoryMock(),
                referencesDAO
        );
        Assertions.assertFalse(bibEntryRelationsRepository.containsReferences(bibEntry));
        Assertions.assertFalse(references.isEmpty());

        // WHEN
        bibEntryRelationsRepository.insertReferences(bibEntry, references);

        // THEN
        Assertions.assertTrue(bibEntryRelationsRepository.containsReferences(bibEntry));
        Assertions.assertEquals(references, bibEntryRelationsRepository.readReferences(bibEntry));
    }

    @Test
    void readReferencesShouldReturnEmptyListWhenEntryIsNull() {
        // GIVEN
        BibEntryCitationsAndReferencesRepositoryShell repository = new BibEntryCitationsAndReferencesRepositoryShell(
                new BibEntryRelationRepositoryMock(),
                new BibEntryRelationRepositoryMock()
        );

        // WHEN
        List<BibEntry> references = repository.readReferences(null);

        // THEN
        Assertions.assertTrue(references.isEmpty());
        Assertions.assertEquals(List.of(), references);
    }

    @Test
    void readCitationsShouldReturnEmptyListWhenEntryIsNull() {
        // GIVEN
        BibEntryCitationsAndReferencesRepositoryShell repository = new BibEntryCitationsAndReferencesRepositoryShell(
                new BibEntryRelationRepositoryMock(),
                new BibEntryRelationRepositoryMock()
        );

        // WHEN
        List<BibEntry> citations = repository.readCitations(null);

        // THEN
        Assertions.assertTrue(citations.isEmpty());
        Assertions.assertEquals(List.of(), citations);
    }

    @Test
    void insertCitationsShouldHandleNullListCorrectly() {
        // GIVEN
        BibEntry targetEntry = createBibEntry();
        BibEntryRelationRepositoryMock citationsDAO = new BibEntryRelationRepositoryMock();
        BibEntryCitationsAndReferencesRepositoryShell repository = new BibEntryCitationsAndReferencesRepositoryShell(
                citationsDAO,
                new BibEntryRelationRepositoryMock()
        );

        // WHEN
        repository.insertCitations(targetEntry, null);

        // THEN
        Assertions.assertTrue(repository.containsCitations(targetEntry));
        Assertions.assertTrue(repository.readCitations(targetEntry).isEmpty());
    }

    @Test
    void isCitationsUpdatableShouldDelegateToCitationsDAO() {
        // GIVEN
        BibEntry bibEntry = createBibEntry();
        BibEntryRelationRepositoryMock citationsDAO = new BibEntryRelationRepositoryMock(true);
        BibEntryCitationsAndReferencesRepositoryShell repository = new BibEntryCitationsAndReferencesRepositoryShell(
                citationsDAO,
                new BibEntryRelationRepositoryMock(false)
        );

        // WHEN/THEN
        Assertions.assertTrue(repository.isCitationsUpdatable(bibEntry));
    }

    @Test
    void isReferencesUpdatableShouldDelegateToReferencesDAO() {
        // GIVEN
        BibEntry bibEntry = createBibEntry();
        BibEntryRelationRepositoryMock referencesDAO = new BibEntryRelationRepositoryMock(true);
        BibEntryCitationsAndReferencesRepositoryShell repository = new BibEntryCitationsAndReferencesRepositoryShell(
                new BibEntryRelationRepositoryMock(false),
                referencesDAO
        );

        // WHEN/THEN
        Assertions.assertTrue(repository.isReferencesUpdatable(bibEntry));
    }

    @Test
    void closeShouldCloseBothUnderlyingDAOs() {
        // GIVEN
        BibEntryRelationRepositoryMock citationsDAO = new BibEntryRelationRepositoryMock();
        BibEntryRelationRepositoryMock referencesDAO = new BibEntryRelationRepositoryMock();
        BibEntryCitationsAndReferencesRepositoryShell repository = new BibEntryCitationsAndReferencesRepositoryShell(
                citationsDAO,
                referencesDAO
        );

        // WHEN
        repository.close();

        // THEN
        Assertions.assertTrue(citationsDAO.wasClosed());
        Assertions.assertTrue(referencesDAO.wasClosed());
    }
}
