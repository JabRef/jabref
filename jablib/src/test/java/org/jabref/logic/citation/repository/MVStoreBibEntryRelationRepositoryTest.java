package org.jabref.logic.citation.repository;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.PaperDetails;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.DisabledOnCIServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisabledOnCIServer("Strange out of memory exceptions, works with manual testing")
class MVStoreBibEntryRelationRepositoryTest {

    private final static String MV_STORE_NAME = "test-relations.mv";
    private final static String MAP_NAME = "test-relations";

    @TempDir
    Path temporaryFolder;

    private MVStoreBibEntryRelationRepository dao;

    @BeforeEach
    void initStore() throws Exception {
        Path file = Files.createFile(temporaryFolder.resolve(MV_STORE_NAME));

        this.dao = new MVStoreBibEntryRelationRepository(
                file.toAbsolutePath(),
                MAP_NAME,
                7,
                new BibEntryTypesManager(),
                mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS),
                mock(FieldPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @AfterEach
    void closeStore() {
        this.dao.close();
        // On the CI, we sometimes get "OutOfMemoryException"s. This tries to prevent that.
        System.gc();
    }

    private static Stream<BibEntry> createBibEntries() {
        return IntStream
                .range(0, 150)
                .mapToObj(MVStoreBibEntryRelationRepositoryTest::createBibEntry);
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
    private List<BibEntry> createRelations(BibEntry entry) {
        return entry
                .getCitationKey()
                .map(key -> RandomGenerator.StreamableGenerator
                        .of("L128X256MixRandom").ints(150)
                        .mapToObj(i -> new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.TITLE, "A title: " + i)
                                .withField(StandardField.YEAR, String.valueOf(2024))
                                .withField(StandardField.AUTHOR, "{A list of authors: " + i + "}")
                                .withField(StandardField.DOI, entry.getDOI().map(DOI::asString).orElse("") + ":" + i)
                                .withField(StandardField.ABSTRACT, "The Universe is expanding: " + i)
                        )
                )
                .orElseThrow()
                .toList();
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void DAOShouldMergeRelationsWhenInserting(BibEntry bibEntry) {
        // GIVEN
        assertFalse(dao.containsKey(bibEntry));
        List<BibEntry> firstRelations = createRelations(bibEntry);
        List<BibEntry> secondRelations = createRelations(bibEntry);

        // WHEN
        dao.addRelations(bibEntry, firstRelations);
        dao.addRelations(bibEntry, secondRelations);
        List<BibEntry> relationFromCache = dao
                .getRelations(bibEntry)
                .stream()
                .toList();

        // THEN
        List<BibEntry> uniqueRelations = Stream
                .concat(firstRelations.stream(), secondRelations.stream())
                .distinct()
                .toList();
        assertFalse(uniqueRelations.isEmpty());
        assertNotSame(uniqueRelations, relationFromCache);
        assertEquals(uniqueRelations, relationFromCache);
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void containsKeyShouldReturnFalseIfNothingWasInserted(BibEntry entry) {
        assertFalse(dao.containsKey(entry));
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void containsKeyShouldReturnTrueIfRelationsWereInserted(BibEntry entry) {
        // GIVEN
        List<BibEntry> relations = createRelations(entry);

        // WHEN
        dao.addRelations(entry, relations);

        // THEN
        assertTrue(dao.containsKey(entry));
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void shouldUpdateShouldReturnTrueBeforeInsertionsAndFalseAfterInsertions(BibEntry entry) {
        // GIVEN
        List<BibEntry> relations = createRelations(entry);
        assertTrue(dao.shouldUpdate(entry));

        // WHEN
        dao.addRelations(entry, relations);

        // THEN
        assertFalse(dao.shouldUpdate(entry));
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void shouldUpdateShouldReturnTrueAfterOneWeek(BibEntry entry) {
        // GIVEN
        List<BibEntry> relations = createRelations(entry);
        Clock clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
        assertTrue(dao.shouldUpdate(entry, clock));

        // WHEN
        dao.addRelations(entry, relations);

        // THEN
        assertFalse(dao.shouldUpdate(entry, clock));
        Clock clockOneWeekAfter = Clock.fixed(
                LocalDateTime.now(ZoneId.of("UTC")).plusWeeks(1).plusSeconds(1).toInstant(ZoneOffset.UTC),
                ZoneId.of("UTC")
        );
        assertTrue(dao.shouldUpdate(entry, clockOneWeekAfter));
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void shouldUpdateShouldReturnFalseAfterOneWeekWhenTTLisSetTo30(BibEntry entry) throws IOException {
        // GIVEN
        List<BibEntry> relations = createRelations(entry);
        Clock clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
        Path file = Files.createFile(temporaryFolder.resolve("update_test" + MV_STORE_NAME));
        MVStoreBibEntryRelationRepository daoUnderTest = new MVStoreBibEntryRelationRepository(
                file.toAbsolutePath(),
                MAP_NAME,
                30,
                mock(BibEntryTypesManager.class, Answers.RETURNS_DEEP_STUBS),
                mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS),
                mock(FieldPreferences.class, Answers.RETURNS_DEEP_STUBS));
        assertTrue(daoUnderTest.shouldUpdate(entry, clock));

        // WHEN
        daoUnderTest.addRelations(entry, relations);

        // THEN
        assertFalse(daoUnderTest.shouldUpdate(entry, clock));
        Clock clockOneWeekAfter = Clock.fixed(
                LocalDateTime.now(ZoneId.of("UTC")).plusWeeks(1).toInstant(ZoneOffset.UTC),
                ZoneId.of("UTC")
        );
        assertFalse(daoUnderTest.shouldUpdate(entry, clockOneWeekAfter));
    }

    @ParameterizedTest
    @MethodSource("createBibEntries")
    void deserializerErrorShouldReturnEmptyList(BibEntry entry) throws IOException {
        // GIVEN
        BibEntryHashSetSerializer serializer = new BibEntryHashSetSerializer(
                new BibEntrySerializer(
                        new BibEntryTypesManager(),
                        mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS),
                        mock(FieldPreferences.class, Answers.RETURNS_DEEP_STUBS)) {
                    @Override
                    public BibEntry read(ByteBuffer buffer) {
                        // Fake the return after an exception
                        return new BibEntry();
                    }
                }
        );
        Path file = Files.createFile(temporaryFolder.resolve("serialization_error_" + MV_STORE_NAME));
        MVStoreBibEntryRelationRepository daoUnderTest = new MVStoreBibEntryRelationRepository(file.toAbsolutePath(), MAP_NAME, 7, serializer);
        List<BibEntry> relations = createRelations(entry);

        // WHEN
        daoUnderTest.addRelations(entry, relations);
        daoUnderTest.close();
        daoUnderTest = new MVStoreBibEntryRelationRepository(file.toAbsolutePath(), MAP_NAME, 7, serializer);
        List<BibEntry> deserializedRelations = daoUnderTest.getRelations(entry);

        // THEN
        assertTrue(deserializedRelations.isEmpty());
    }
}
