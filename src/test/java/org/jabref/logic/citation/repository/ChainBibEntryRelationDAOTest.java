package org.jabref.logic.citation.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jabref.model.citation.semanticscholar.PaperDetails;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ChainBibEntryRelationDAOTest {

    private static Stream<BibEntry> createBibEntries() {
        return IntStream
            .range(0, 150)
            .mapToObj(ChainBibEntryRelationDAOTest::createBibEntry);
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
     * @param entry should not be null
     * @return never empty
     */
    private static List<BibEntry> createRelations(BibEntry entry) {
        return entry
            .getCitationKey()
            .map(key -> RandomGenerator.StreamableGenerator
                .of("L128X256MixRandom").ints(150)
                .mapToObj(i -> new BibEntry()
                    .withField(StandardField.TITLE, "A title:" + i)
                    .withField(StandardField.YEAR, String.valueOf(2024))
                    .withField(StandardField.AUTHOR, "A list of authors:" + i)
                    .withType(StandardEntryType.Book)
                    .withField(StandardField.DOI, entry.getDOI().map(DOI::getDOI).orElse("") + ":" + i)
                    .withField(StandardField.URL, "www.jabref.org/" + i)
                    .withField(StandardField.ABSTRACT, "The Universe is expanding:" + i)
                )
            )
            .orElseThrow()
            .collect(Collectors.toList());
    }

    private static Stream<Arguments> createCacheAndBibEntry() {
        return Stream
            .of(LRUCacheBibEntryRelationsDAO.CITATIONS, LRUCacheBibEntryRelationsDAO.REFERENCES)
            .flatMap(dao -> {
                dao.clearEntries();
                return createBibEntries().map(entry -> Arguments.of(dao, entry));
            });
    }

    private class DaoMock implements BibEntryRelationDAO {

        Map<BibEntry, List<BibEntry>> table = new HashMap<>();

        @Override
        public List<BibEntry> getRelations(BibEntry entry) {
            return this.table.getOrDefault(entry, List.of());
        }

        @Override
        public void cacheOrMergeRelations(BibEntry entry, List<BibEntry> relations) {
            this.table.put(entry, relations);
        }

        @Override
        public boolean containsKey(BibEntry entry) {
            return this.table.containsKey(entry);
        }
    }

    @ParameterizedTest
    @MethodSource("createCacheAndBibEntry")
    void theChainShouldReadFromFirstNode(BibEntryRelationDAO dao, BibEntry entry) {
        // GIVEN
        var relations = createRelations(entry);
        dao.cacheOrMergeRelations(entry, relations);
        var secondDao = new DaoMock();
        var doaChain = ChainBibEntryRelationDAO.of(dao, secondDao);

        // WHEN
        var relationsFromChain = doaChain.getRelations(entry);

        // THEN
        Assertions.assertEquals(relations, relationsFromChain);
        Assertions.assertEquals(relations, dao.getRelations(entry));
    }

    @ParameterizedTest
    @MethodSource("createCacheAndBibEntry")
    void theChainShouldReadFromSecondNode(BibEntryRelationDAO dao, BibEntry entry) {
        // GIVEN
        var relations = createRelations(entry);
        dao.cacheOrMergeRelations(entry, relations);
        var firstDao = new DaoMock();
        var doaChain = ChainBibEntryRelationDAO.of(firstDao, dao);

        // WHEN
        var relationsFromChain = doaChain.getRelations(entry);

        // THEN
        Assertions.assertEquals(relations, relationsFromChain);
        Assertions.assertEquals(relations, dao.getRelations(entry));
    }

    @ParameterizedTest
    @MethodSource("createCacheAndBibEntry")
    void theChainShouldReadFromSecondNodeAndRecopyToFirstNode(BibEntryRelationDAO dao, BibEntry entry) {
        // GIVEN
        var relations = createRelations(entry);
        var firstDao = new DaoMock();
        var doaChain = ChainBibEntryRelationDAO.of(firstDao, dao);

        // WHEN
        doaChain.cacheOrMergeRelations(entry, relations);
        var relationsFromChain = doaChain.getRelations(entry);

        // THEN
        Assertions.assertEquals(relations, relationsFromChain);
        Assertions.assertEquals(relations, firstDao.getRelations(entry));
        Assertions.assertEquals(relations, dao.getRelations(entry));
    }

    @ParameterizedTest
    @MethodSource("createCacheAndBibEntry")
    void theChainShouldContainAKeyEvenIfItWasOnlyInsertedInLastNode(BibEntryRelationDAO dao, BibEntry entry) {
        // GIVEN
        var relations = createRelations(entry);
        var firstDao = new DaoMock();
        var doaChain = ChainBibEntryRelationDAO.of(firstDao, dao);

        // WHEN
        dao.cacheOrMergeRelations(entry, relations);
        Assertions.assertFalse(firstDao.containsKey(entry));
        boolean doesChainContainsTheKey = doaChain.containsKey(entry);

        // THEN
        Assertions.assertTrue(doesChainContainsTheKey);
    }
}
