package org.jabref.logic.citation.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.google.common.annotations.VisibleForTesting;
import org.h2.mvstore.MVStore;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class BibEntryCitationsAndReferencesRepositoryShell implements BibEntryCitationsAndReferencesRepository, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibEntryCitationsAndReferencesRepositoryShell.class);

    private static final String CITATION_RELATIONS_STORE = "citation-relations.mv";

    @Nullable // when testing constructor is used
    private final MVStore mvStore;

    private final BibEntryRelationRepository citationsDao;
    private final BibEntryRelationRepository referencesDao;

    public BibEntryCitationsAndReferencesRepositoryShell(Path citationsRelationsDirectory,
                                                                   int storeTTL,
                                                                   ImportFormatPreferences importFormatPreferences,
                                                                   FieldPreferences fieldPreferences,
                                                                   BibEntryTypesManager entryTypesManager,
                                                                   ObjectProperty<CitationFetcherType> citationFetcherTypeProperty) {
        Path storePath = citationsRelationsDirectory.resolve(CITATION_RELATIONS_STORE);
        try {
            Files.createDirectories(storePath.getParent());
            if (!Files.exists(storePath)) {
                Files.createFile(storePath);
            }
        } catch (IOException e) {
            LOGGER.error("An error occurred while opening {} storage", storePath, e);
        }

        mvStore = new MVStore.Builder()
                .fileName(storePath.toAbsolutePath().toString())
                .open();

        this.referencesDao = new MVStoreBibEntryRelationRepository(mvStore, "references", storeTTL, entryTypesManager, importFormatPreferences, fieldPreferences, citationFetcherTypeProperty);
        this.citationsDao = new MVStoreBibEntryRelationRepository(mvStore, "citations", storeTTL, entryTypesManager, importFormatPreferences, fieldPreferences, citationFetcherTypeProperty);
    }

    @VisibleForTesting
    public BibEntryCitationsAndReferencesRepositoryShell(
            BibEntryRelationRepository citationsDao,
            BibEntryRelationRepository referencesDao
    ) {
        this.citationsDao = citationsDao;
        this.referencesDao = referencesDao;
        this.mvStore = null;
    }

    @Override
    public void addCitations(BibEntry entry, List<BibEntry> citations) {
        citationsDao.addRelations(
                entry, Objects.requireNonNullElseGet(citations, List::of)
        );
    }

    @Override
    public List<BibEntry> getCitations(@Nullable BibEntry entry) {
        if (entry == null) {
            return List.of();
        }
        return citationsDao.getRelations(entry);
    }

    @Override
    public boolean containsCitations(BibEntry entry) {
        return citationsDao.containsKey(entry);
    }

    @Override
    public boolean isCitationsUpdatable(BibEntry entry) {
        return citationsDao.shouldUpdate(entry);
    }

    @Override
    public void addReferences(BibEntry entry, @Nullable List<BibEntry> references) {
        referencesDao.addRelations(
                entry, Objects.requireNonNullElseGet(references, List::of)
        );
    }

    @Override
    public List<BibEntry> getReferences(@Nullable BibEntry entry) {
        if (entry == null) {
            return List.of();
        }
        return referencesDao.getRelations(entry);
    }

    @Override
    public boolean containsReferences(BibEntry entry) {
        return referencesDao.containsKey(entry);
    }

    @Override
    public boolean isReferencesUpdatable(BibEntry entry) {
        return referencesDao.shouldUpdate(entry);
    }

    @Override
    public void close() {
        this.mvStore.close();
    }
}
