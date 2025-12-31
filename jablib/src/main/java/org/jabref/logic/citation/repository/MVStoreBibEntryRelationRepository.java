package org.jabref.logic.citation.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;

import javafx.beans.property.ObjectProperty;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.google.common.annotations.VisibleForTesting;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
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
    private final static Clock CLOCK = Clock.system(TIME_STAMP_ZONE_ID);
    private final static String MAP_SUFFIX_TIME_STAMP = "-insertion-timestamp";

    private final int storeTTLInDays;
    private final MVMap<String, LinkedHashSet<BibEntry>> relationsMap;
    private final MVMap<String, LocalDateTime> insertionTimeStampMap;

    // should only be read for closing - all other maps initialized at constructor
    private final MVStore store;

    private final ObjectProperty<CitationFetcherType> citationFetcherPropertyType;

    @VisibleForTesting
    MVStoreBibEntryRelationRepository(Path path,
                                      String mapName,
                                      int storeTTLInDays,
                                      BibEntryTypesManager entryTypesManager,
                                      ImportFormatPreferences importFormatPreferences,
                                      FieldPreferences fieldPreferences,
                                      ObjectProperty<CitationFetcherType> citationFetcherTypeProperty) {
        this(
                path,
                mapName,
                storeTTLInDays,
                new BibEntryHashSetSerializer(entryTypesManager, importFormatPreferences, fieldPreferences),
                citationFetcherTypeProperty
        );
    }

    @VisibleForTesting
    MVStoreBibEntryRelationRepository(Path path,
                                      String mapName,
                                      int storeTTLInDays,
                                      BasicDataType<LinkedHashSet<BibEntry>> serializer,
                                      ObjectProperty<CitationFetcherType> citationFetcherTypeProperty) {
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            LOGGER.error("An error occurred while opening {} storage", mapName, e);
        }

        this.storeTTLInDays = storeTTLInDays;

        MVMap.Builder<String, LinkedHashSet<BibEntry>> mapConfiguration = new MVMap.Builder<String, LinkedHashSet<BibEntry>>().valueType(serializer);
        // We can rely on the auto commit features, because we have only one store reader
        store = new MVStore.Builder()
                .fileName(path.toAbsolutePath().toString())
                .open();
        this.relationsMap = store.openMap(mapName, mapConfiguration);
        this.insertionTimeStampMap = store.openMap(mapName + MAP_SUFFIX_TIME_STAMP);
        this.citationFetcherPropertyType = citationFetcherTypeProperty;
    }

    @Override
    public List<BibEntry> getRelations(BibEntry entry) {
        return entry
                .getDOI()
                .map(doi -> relationsMap.getOrDefault(doi.asString() + citationFetcherPropertyType.get().getName(), new LinkedHashSet<>()).stream().toList())
                .orElse(List.of());
    }

    /**
     * Allows insertion of empty list in order to keep track of insertion date for an entry.
     */
    @Override
    synchronized public void addRelations(@NonNull BibEntry entry, @NonNull List<BibEntry> relations) {
        entry.getDOI().ifPresent(doi -> {
            if (!relations.isEmpty()) {
                // Save the relations
                LinkedHashSet<BibEntry> relationsAlreadyStored = relationsMap.getOrDefault(doi.asString() + citationFetcherPropertyType.get().getName(), new LinkedHashSet<>());
                relationsAlreadyStored.addAll(relations);
                relationsMap.put(doi.asString() + citationFetcherPropertyType.get().getName(), relationsAlreadyStored);
            }

            // Save insertion timestamp
            LocalDateTime insertionTime = LocalDateTime.now(TIME_STAMP_ZONE_ID);
            insertionTimeStampMap.put(doi.asString() + citationFetcherPropertyType.get().getName(), insertionTime);
        });
    }

    @Override
    synchronized public boolean containsKey(BibEntry entry) {
        return entry
                .getDOI()
                .map(doi -> relationsMap.containsKey(doi.asString() + citationFetcherPropertyType.get().getName()))
                .orElse(false);
    }

    @Override
    synchronized public boolean shouldUpdate(BibEntry entry) {
        return shouldUpdate(entry, CLOCK);
    }

    @VisibleForTesting
    boolean shouldUpdate(final BibEntry entry, final Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        return entry.getDOI()
                    .map(doi -> {
                        String doiString = doi.asString();
                        if (!insertionTimeStampMap.containsKey(doiString + citationFetcherPropertyType.get().getName())) {
                            return true;
                        }
                        LocalDateTime lastRun = insertionTimeStampMap.get(doiString + citationFetcherPropertyType.get().getName());
                        return lastRun.isBefore(now.minusDays(storeTTLInDays));
                    })
                    // No DOI existing - allow update
                    .orElse(true);
    }

    @Override
    public void close() {
        this.store.close();
    }
}
