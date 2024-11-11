package org.jabref.logic.citation.repository;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jabref.model.citation.semanticscholar.PaperDetails;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MVStoreBibEntryRelationsRepositoryTest {

    private static Stream<BibEntry> createBibEntries() {
        return IntStream
            .range(0, 150)
            .mapToObj(MVStoreBibEntryRelationsRepositoryTest::createBibEntry);
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
    private static LinkedHashSet<BibEntry> createRelations(BibEntry entry) {
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
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static class BibEntrySerializer extends BasicDataType<BibEntry> {

        private final static String FIELD_SEPARATOR = "--";

        private static String toString(BibEntry entry) {
            return String.join(
                FIELD_SEPARATOR,
                entry.getTitle().orElse("null"),
                entry.getField(StandardField.YEAR).orElse("null"),
                entry.getField(StandardField.AUTHOR).orElse("null"),
                entry.getType().getDisplayName(),
                entry.getDOI().map(DOI::getDOI).orElse("null"),
                entry.getField(StandardField.URL).orElse("null"),
                entry.getField(StandardField.ABSTRACT).orElse("null")
            );
        }

        private static Optional<String> extractFieldValue(String field) {
            return Objects.equals(field, "null") || field == null
                ? Optional.empty()
                : Optional.of(field);
        }

        private static BibEntry fromString(String serializedString) {
            var fields = serializedString.split(FIELD_SEPARATOR);
            BibEntry entry = new BibEntry();
            extractFieldValue(fields[0]).ifPresent(title -> entry.setField(StandardField.TITLE, title));
            extractFieldValue(fields[1]).ifPresent(year -> entry.setField(StandardField.YEAR, year));
            extractFieldValue(fields[2]).ifPresent(authors -> entry.setField(StandardField.AUTHOR, authors));
            extractFieldValue(fields[3]).ifPresent(type -> entry.setType(StandardEntryType.valueOf(type)));
            extractFieldValue(fields[4]).ifPresent(doi -> entry.setField(StandardField.DOI, doi));
            extractFieldValue(fields[5]).ifPresent(url -> entry.setField(StandardField.URL, url));
            extractFieldValue(fields[6])
                .ifPresent(entryAbstract -> entry.setField(StandardField.ABSTRACT, entryAbstract));
            return entry;
        }

        @Override
        public int getMemory(BibEntry obj) {
            return toString(obj).getBytes(StandardCharsets.UTF_8).length;
        }

        @Override
        public void write(WriteBuffer buff, BibEntry bibEntry) {
            var asBytes = toString(bibEntry).getBytes(StandardCharsets.UTF_8);
            buff.putInt(asBytes.length);
            buff.put(asBytes);
        }

        @Override
        public BibEntry read(ByteBuffer buff) {
            int serializedEntrySize = buff.getInt();
            var serializedEntry = DataUtils.readString(buff, serializedEntrySize);
            return fromString(serializedEntry);
        }

        @Override
        public int compare(BibEntry a, BibEntry b) {
            if (a == null || b == null) {
                throw new NullPointerException();
            }
            return toString(a).compareTo(toString(b));
        }

        @Override
        public BibEntry[] createStorage(int size) {
            return new BibEntry[size];
        }
    }

    static class BibEntryHashSetSerializer extends BasicDataType<LinkedHashSet<BibEntry>> {

        private final BasicDataType<BibEntry> bibEntryDataType = new BibEntrySerializer();

        /**
         * Memory size is the sum of all aggregated bibEntries memory size plus 4 bytes.
         * Those 4 bytes are used to store the length of the collection itself.
         * @param bibEntries should not be null
         * @return total size in memory of the serialized collection of bib entries
         */
        @Override
        public int getMemory(LinkedHashSet<BibEntry> bibEntries) {
            return bibEntries
                .stream()
                .map(this.bibEntryDataType::getMemory)
                .reduce(0, Integer::sum) + 4;
        }

        @Override
        public void write(WriteBuffer buff, LinkedHashSet<BibEntry> bibEntries) {
            buff.putInt(bibEntries.size());
            bibEntries.forEach(entry -> this.bibEntryDataType.write(buff, entry));
        }

        @Override
        public LinkedHashSet<BibEntry> read(ByteBuffer buff) {
            return IntStream.range(0, buff.getInt())
                .mapToObj(it -> this.bibEntryDataType.read(buff))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        @Override
        public LinkedHashSet<BibEntry>[] createStorage(int size) {
            return new LinkedHashSet[size];
        }
    }

    @Test
    void itShouldBePossibleToStoreABibEntryList(@TempDir Path temporaryFolder) throws IOException {
        var file = Files.createFile(temporaryFolder.resolve("test_string_store"));
        try (var store = new MVStore.Builder().fileName(file.toAbsolutePath().toString()).open()) {
            // GIVEN
            MVMap<String, List<String>> citations = store.openMap("citations");

            // WHEN
            citations.put("Hello", List.of("The", "World"));
            store.commit();
            var fromStore = citations.get("Hello");

            // THEN
            Assertions.assertTrue(Files.exists(file));
            Assertions.assertEquals("Hello The World", "Hello " + String.join(" ", fromStore));
        }
    }

    /**
     * Fake in memory sequential save and load
     */
    @Test
    void IWouldLikeToSaveAndLoadCitationsForABibEntryFromAMap(@TempDir Path temporaryFolder) throws IOException {
        var file = Files.createFile(temporaryFolder.resolve("bib_entry_citations_test_store"));
        try (var store = new MVStore.Builder().fileName(file.toAbsolutePath().toString()).open()) {
            // GIVEN
            Map<String, LinkedHashSet<BibEntry>> citationsToBeStored = createBibEntries()
                .map(e -> Pair.of(e.getDOI().orElseThrow().getDOI(), createRelations(e)))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
            Assertions.assertFalse(citationsToBeStored.isEmpty());
            var mapConfiguration = new MVMap.Builder<String, LinkedHashSet<BibEntry>>()
                .valueType(new BibEntryHashSetSerializer());

            /**
            var mapConfiguration = new MVMap.Builder<String, LinkedHashSet<BibEntry>>()
                .valueType(new BibEntryHashSetSerializer());
            MVMap<String, LinkedHashSet<BibEntry>> citationsMap = store.openMap("citations", mapConfiguration);
            **/
            MVMap<String, LinkedHashSet<BibEntry>> citationsMap = store.openMap("citations", mapConfiguration);

            // WHEN
            citationsToBeStored.forEach((entry, citations) -> citationsMap.put(entry, new LinkedHashSet<>(citations)));

            // THEN
            citationsToBeStored.forEach((entry, citations) -> {
                Assertions.assertTrue(citationsMap.containsKey(entry));
                Assertions.assertEquals(citations, citationsMap.get(entry));
            });
        }
    }

    /**
     * Fake persisted sequential save and load operations.
     */
    @Test
    void IWouldLikeToSaveAndLoadCitationsForABibEntryFromAStore(@TempDir Path temporaryFolder) throws IOException {
        var file = Files.createFile(temporaryFolder.resolve("bib_entry_citations_test_store"));

        // GIVEN
        Map<String, LinkedHashSet<BibEntry>> citationsToBeStored = createBibEntries()
            .map(e -> Pair.of(e.getDOI().orElseThrow().getDOI(), createRelations(e)))
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        Assertions.assertFalse(citationsToBeStored.isEmpty());

        var mapConfiguration = new MVMap.Builder<String, LinkedHashSet<BibEntry>>()
            .valueType(new BibEntryHashSetSerializer());

        Map<String, LinkedHashSet<BibEntry>> citationsFromStore = null;

        // WHEN
        // STORING AND CLOSING
        try (var store = new MVStore.Builder().fileName(file.toAbsolutePath().toString()).open()) {
            MVMap<String, LinkedHashSet<BibEntry>> citationsMap = store.openMap("citations", mapConfiguration);
            citationsToBeStored.forEach((entry, citations) -> citationsMap.put(entry, new LinkedHashSet<>(citations)));
            store.commit();
        }

        // READING AND CLOSING
        try (var store = new MVStore.Builder().fileName(file.toAbsolutePath().toString()).open()) {
            MVMap<String, LinkedHashSet<BibEntry>> citationsMap = store.openMap("citations", mapConfiguration);
            citationsFromStore = Map.copyOf(citationsMap);
        }

        // THEN
        Assertions.assertNotNull(citationsFromStore);
        Assertions.assertFalse(citationsFromStore.isEmpty());
        var entriesToBeStored = citationsToBeStored.entrySet();
        for (var entry : entriesToBeStored) {
            Assertions.assertTrue(citationsFromStore.containsKey(entry.getKey()));
            var citations = citationsFromStore.get(entry.getKey());
            Assertions.assertEquals(entry.getValue(), citations);
        }
    }
}
