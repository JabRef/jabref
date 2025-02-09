package org.jabref.logic.citation.repository;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jabref.model.citation.semanticscholar.PaperDetails;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LRUCacheBibEntryRelationsRepositoryTest {

    private static Stream<BibEntry> createBibEntries() {
        return IntStream
            .range(0, 150)
            .mapToObj(LRUCacheBibEntryRelationsRepositoryTest::createBibEntry);
    }

    private static BibEntry createBibEntry(int i) {
        return new BibEntry()
            .withCitationKey(String.valueOf(i))
            .withField(StandardField.DOI, "10.1234/5678" + i);
    }

    /**
     * Create a fake list of relations for a bibEntry based on the {@link PaperDetails#toBibEntry()} logic
     * that corresponds to this use case: we want to make sure that relations coming from SemanticScholar
     * and mapped as BibEntry will be serializable by the MVStore.
     *
     * @param entry should not be null
     * @return never empty
     */
    private static List<BibEntry> createRelations(BibEntry entry) {
        return entry
            .getCitationKey()
            .map(key -> RandomGenerator.StreamableGenerator
                .of("L128X256MixRandom").ints(150)
                .mapToObj(i -> new BibEntry()
                    .withField(StandardField.TITLE, "A title: " + i)
                    .withField(StandardField.YEAR, String.valueOf(2024))
                    .withField(StandardField.AUTHOR, "{A list of authors: " + i + "}")
                    .withType(StandardEntryType.Book)
                    .withField(StandardField.DOI, entry.getDOI().map(DOI::asString).orElse("") + ":" + i)
                    .withField(StandardField.ABSTRACT, "The Universe is expanding: " + i)
                )
            )
            .orElseThrow()
            .collect(Collectors.toList());
    }

    private static Stream<Arguments> createCacheAndBibEntry() {
        return Stream
            .of(LRUCacheBibEntryRelationsRepository.CITATIONS, LRUCacheBibEntryRelationsRepository.REFERENCES)
            .flatMap(dao -> createBibEntries().map(entry -> Arguments.of(dao, entry)));
    }

    @ParameterizedTest
    @MethodSource("createCacheAndBibEntry")
    void repositoryShouldMergeCitationsWhenInserting(LRUCacheBibEntryRelationsRepository dao, BibEntry entry) {
        // GIVEN
        dao.clearEntries();
        assertFalse(dao.containsKey(entry));

        // WHEN
        var firstRelations = createRelations(entry);
        var secondRelations = createRelations(entry);
        dao.addRelations(entry, firstRelations);
        dao.addRelations(entry, secondRelations);

        // THEN
        var uniqueRelations = Stream
            .concat(firstRelations.stream(), secondRelations.stream())
            .distinct()
            .toList();
        var relationFromCache = dao.getRelations(entry);
        assertTrue(dao.containsKey(entry));
        assertFalse(uniqueRelations.isEmpty());
        assertNotSame(uniqueRelations, relationFromCache);
        assertEquals(uniqueRelations, relationFromCache);
    }

    @ParameterizedTest
    @MethodSource("createCacheAndBibEntry")
    void clearingCacheShouldWork(LRUCacheBibEntryRelationsRepository dao, BibEntry entry) {
        // GIVEN
        dao.clearEntries();
        var relations = createRelations(entry);
        assertFalse(dao.containsKey(entry));

        // WHEN
        dao.addRelations(entry, relations);
        assertTrue(dao.containsKey(entry));
        dao.clearEntries();

        // THEN
        assertFalse(dao.containsKey(entry));
    }
}
