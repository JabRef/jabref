package org.jabref.logic.citation.repository;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class LRUBibEntryRelationsRepositoryTest {

    private static Stream<BibEntry> createBibEntries() {
        return IntStream
            .range(0, 150)
            .mapToObj(LRUBibEntryRelationsRepositoryTest::createBibEntry);
    }

    private static BibEntry createBibEntry(int i) {
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

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void repositoryShouldMergeCitationsWhenInserting(BibEntry bibEntry) {
        // GIVEN
        var bibEntryRelationsRepository = new LRUBibEntryRelationsRepository(
            new LRUBibEntryRelationsCache()
        );
        assertFalse(bibEntryRelationsRepository.containsCitations(bibEntry));

        // WHEN
        var firstRelations = createRelations(bibEntry);
        var secondRelations = createRelations(bibEntry);
        bibEntryRelationsRepository.insertCitations(bibEntry, firstRelations);
        bibEntryRelationsRepository.insertCitations(bibEntry, secondRelations);

        // THEN
        var uniqueRelations = Stream
            .concat(firstRelations.stream(), secondRelations.stream())
            .distinct()
            .toList();
        var relationFromCache = bibEntryRelationsRepository.readCitations(bibEntry);
        assertFalse(uniqueRelations.isEmpty());
        assertNotSame(uniqueRelations, relationFromCache);
        assertEquals(uniqueRelations, relationFromCache);
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void repositoryShouldMergeReferencesWhenInserting(BibEntry bibEntry) {
        // GIVEN
        var bibEntryRelationsRepository = new LRUBibEntryRelationsRepository(
            new LRUBibEntryRelationsCache()
        );
        assertFalse(bibEntryRelationsRepository.containsReferences(bibEntry));

        // WHEN
        var firstRelations = createRelations(bibEntry);
        var secondRelations = createRelations(bibEntry);
        bibEntryRelationsRepository.insertReferences(bibEntry, firstRelations);
        bibEntryRelationsRepository.insertReferences(bibEntry, secondRelations);

        // THEN
        var uniqueRelations = Stream
            .concat(firstRelations.stream(), secondRelations.stream())
            .distinct()
            .collect(Collectors.toList());
        var relationFromCache = bibEntryRelationsRepository.readReferences(bibEntry);
        assertFalse(uniqueRelations.isEmpty());
        assertNotSame(uniqueRelations, relationFromCache);
        assertEquals(uniqueRelations, relationFromCache);
    }
}
