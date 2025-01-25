package org.jabref.logic.citation.repository;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.annotations.VisibleForTesting;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MVStoreBibEntryRelationDAO implements BibEntryRelationDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(MVStoreBibEntryRelationDAO.class);
    private final static ZoneId TIME_STAMP_ZONE_ID = ZoneId.of("UTC");
    private final static String TIME_STAMP_SUFFIX = "-insertion-timestamp";

    private final String mapName;
    private final String insertionTimeStampMapName;
    private final MVStore.Builder storeConfiguration;
    private final MVMap.Builder<String, LinkedHashSet<BibEntry>> mapConfiguration =
        new MVMap.Builder<String, LinkedHashSet<BibEntry>>().valueType(new BibEntryHashSetSerializer());
    private final int storeTTLInDays;

    MVStoreBibEntryRelationDAO(Path path, String mapName, int storeTTLInDays) {
        try {
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            LOGGER.error("An error occurred while opening {} storage", mapName, e);
        }

        this.mapName = mapName;
        this.insertionTimeStampMapName = mapName + TIME_STAMP_SUFFIX;
        this.storeConfiguration = new MVStore.Builder()
                .autoCommitDisabled()
                .fileName(path.toAbsolutePath().toString());
        this.storeTTLInDays = storeTTLInDays;
    }

    @Override
    public List<BibEntry> getRelations(BibEntry entry) {
        return entry
            .getDOI()
            .map(doi -> {
                try (var store = this.storeConfiguration.open()) {
                    MVMap<String, LinkedHashSet<BibEntry>> relationsMap = store.openMap(mapName, mapConfiguration);
                    return relationsMap.getOrDefault(doi.asString(), new LinkedHashSet<>()).stream().toList();
                }
            })
            .orElse(List.of());
    }

    /**
     * Allows insertion of empty list in order to keep track of insertion date for an entry.
     *
     * @param entry should not be null
     * @param relations should not be null
     */
    @Override
    synchronized public void cacheOrMergeRelations(BibEntry entry, List<BibEntry> relations) {
        entry.getDOI().ifPresent(doi -> {
            try (var store = this.storeConfiguration.open()) {
                // Save the relations
                MVMap<String, LinkedHashSet<BibEntry>> relationsMap = store.openMap(mapName, mapConfiguration);
                var relationsAlreadyStored = relationsMap.getOrDefault(doi.asString(), new LinkedHashSet<>());
                relationsAlreadyStored.addAll(relations);
                relationsMap.put(doi.asString(), relationsAlreadyStored);

                // Save insertion timestamp
                var insertionTime = LocalDateTime.now(TIME_STAMP_ZONE_ID);
                MVMap<String, LocalDateTime> insertionTimeStampMap = store.openMap(insertionTimeStampMapName);
                insertionTimeStampMap.put(doi.asString(), insertionTime);

                // Commit
                store.commit();
            }
        });
    }

    @Override
    synchronized public boolean containsKey(BibEntry entry) {
        return entry
            .getDOI()
            .map(doi -> {
                try (var store = this.storeConfiguration.open()) {
                    MVMap<String, LinkedHashSet<BibEntry>> relationsMap = store.openMap(mapName, mapConfiguration);
                    return relationsMap.containsKey(doi.asString());
                }
            })
            .orElse(false);
    }

    @Override
    synchronized public boolean isUpdatable(BibEntry entry) {
        var clock = Clock.system(TIME_STAMP_ZONE_ID);
        return this.isUpdatable(entry, clock);
    }

    @VisibleForTesting
    boolean isUpdatable(final BibEntry entry, final Clock clock) {
        final var executionTime = LocalDateTime.now(clock);
        return entry
            .getDOI()
            .map(doi -> {
                try (var store = this.storeConfiguration.open()) {
                    MVMap<String, LocalDateTime> insertionTimeStampMap = store.openMap(insertionTimeStampMapName);
                    return insertionTimeStampMap.getOrDefault(doi.asString(), executionTime);
                }
            })
            .map(lastExecutionTime -> lastExecutionTime.equals(executionTime)
                || lastExecutionTime.isBefore(executionTime.minusDays(this.storeTTLInDays))
            )
            .orElse(true);
    }

    private static class BibEntrySerializer extends BasicDataType<BibEntry> {

        private final static String FIELD_SEPARATOR = "--";

        private static String toString(BibEntry entry) {
            return String.join(
                FIELD_SEPARATOR,
                entry.getTitle().orElse("null"),
                entry.getField(StandardField.YEAR).orElse("null"),
                entry.getField(StandardField.AUTHOR).orElse("null"),
                entry.getType().getDisplayName() == null ? "null" : entry.getType().getDisplayName(),
                entry.getDOI().map(DOI::asString).orElse("null"),
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
            var serializedEntry = new byte[serializedEntrySize];
            buff.get(serializedEntry);
            return fromString(new String(serializedEntry, StandardCharsets.UTF_8));
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

        @Override
        public boolean isMemoryEstimationAllowed() {
            return false;
        }
    }

    private static class BibEntryHashSetSerializer extends BasicDataType<LinkedHashSet<BibEntry>> {

        private final BasicDataType<BibEntry> bibEntryDataType = new BibEntrySerializer();

        /**
         * Memory size is the sum of all aggregated bibEntries' memory size plus 4 bytes.
         * Those 4 bytes are used to store the length of the collection itself.
         *
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
        @SuppressWarnings("unchecked")
        public LinkedHashSet<BibEntry>[] createStorage(int size) {
            return new LinkedHashSet[size];
        }

        @Override
        public boolean isMemoryEstimationAllowed() {
            return false;
        }
    }
}
