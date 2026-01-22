package org.jabref.logic.citation.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.random.RandomGenerator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
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
                        .StreamableGenerator.of("L128X256MixRandom").ints(150)
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

        @Override
        public List<BibEntry> getRelations(BibEntry entry) {
            return new ArrayList<>(this.relations.get(entry));
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
        public void close() {
            // do nothing
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
        Assertions.assertFalse(bibEntryRelationsRepository.containsCitations(bibEntry));
        Assertions.assertFalse(references.isEmpty());

        // WHEN
        bibEntryRelationsRepository.insertCitations(bibEntry, references);

        // THEN
        Assertions.assertTrue(bibEntryRelationsRepository.containsCitations(bibEntry));
        Assertions.assertEquals(references, bibEntryRelationsRepository.readCitations(bibEntry));
    }
}
