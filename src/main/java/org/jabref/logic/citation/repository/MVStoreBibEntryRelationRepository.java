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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.UnknownField;

import com.google.common.annotations.VisibleForTesting;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for storing and retrieving relations between BibEntry objects.
 * It uses an MVStore to store the relations.
 */
public class MVStoreBibEntryRelationRepository implements BibEntryRelationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MVStoreBibEntryRelationRepository.class);

    private final static ZoneId TIME_STAMP_ZONE_ID = ZoneId.of("UTC");
    private final static String TIME_STAMP_SUFFIX = "-insertion-timestamp";

    private final String mapName;
    private final String insertionTimeStampMapName;
    private final MVStore.Builder storeConfiguration;
    private final int storeTTLInDays;
    private final MVMap.Builder<String, LinkedHashSet<BibEntry>> mapConfiguration;

    MVStoreBibEntryRelationRepository(Path path, String mapName, int storeTTLInDays) {
        this(
            path,
            mapName,
            storeTTLInDays,
            new MVStoreBibEntryRelationRepository.BibEntryHashSetSerializer()
        );
    }

    MVStoreBibEntryRelationRepository(
        Path path, String mapName, int storeTTLInDays, BasicDataType<LinkedHashSet<BibEntry>> serializer
    ) {
        try {
            Files.createDirectories(path.getParent());
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
        this.mapConfiguration = new MVMap.Builder<String, LinkedHashSet<BibEntry>>().valueType(serializer);
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
     */
    @Override
    synchronized public void addRelations(@NonNull BibEntry entry, @NonNull List<BibEntry> relations) {
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

    static class BibEntrySerializer extends BasicDataType<BibEntry> {

        private final List<Field> fieldsToRemoveFromSerializedEntry = List.of(new UnknownField("_jabref_shared"));

        private static String toString(BibEntry entry) {
            return entry.toString();
        }

        private static Optional<BibEntry> fromString(String serializedString, List<Field> fieldsToRemove) {
            try {
                var importFormatPreferences = new ImportFormatPreferences(
                    new BibEntryPreferences('$'), null, null, null, null, null
                );
                return BibtexParser
                    .singleFromString(serializedString, importFormatPreferences)
                    .map(entry -> {
                        fieldsToRemove.forEach(entry::clearField);
                        return entry;
                    });
            } catch (ParseException e) {
                LOGGER.error("An error occurred while parsing from relation MV store.", e);
                return Optional.empty();
            }
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
            return fromString(
                    new String(serializedEntry, StandardCharsets.UTF_8),
                    this.fieldsToRemoveFromSerializedEntry
                )
                .orElse(new BibEntry());
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

    static class BibEntryHashSetSerializer extends BasicDataType<LinkedHashSet<BibEntry>> {

        private final BasicDataType<BibEntry> bibEntryDataType;

        BibEntryHashSetSerializer() {
            this.bibEntryDataType = new BibEntrySerializer();
        }

        BibEntryHashSetSerializer(BasicDataType<BibEntry> bibEntryDataType) {
            this.bibEntryDataType = bibEntryDataType;
        }

        @Override
        public int getMemory(LinkedHashSet<BibEntry> bibEntries) {
            // Memory size is the sum of all aggregated bibEntries' memory size plus 4 bytes.
            // Those 4 bytes are used to store the length of the collection itself.
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
                            .filter(entry -> !entry.isEmpty())
                            .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        @Override
        @SuppressWarnings("unchecked")
        public LinkedHashSet<BibEntry>[] createStorage(int size) {
            return (LinkedHashSet<BibEntry>[]) new LinkedHashSet[size];
        }

        @Override
        public boolean isMemoryEstimationAllowed() {
            return false;
        }
    }
}
