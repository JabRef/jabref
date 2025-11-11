package org.jabref.logic.citation.repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

public class BibEntryCitationsAndReferencesRepositoryShell implements BibEntryCitationsAndReferencesRepository {

    private static final String CITATIONS_STORE = "citations";
    private static final String REFERENCES_STORE = "references";

    private final BibEntryRelationRepository citationsDao;
    private final BibEntryRelationRepository referencesDao;

    public BibEntryCitationsAndReferencesRepositoryShell(
            BibEntryRelationRepository citationsDao,
            BibEntryRelationRepository referencesDao
    ) {
        this.citationsDao = citationsDao;
        this.referencesDao = referencesDao;
    }

    @Override
    public void insertCitations(BibEntry entry, List<BibEntry> citations) {
        citationsDao.addRelations(
                entry, Objects.requireNonNullElseGet(citations, List::of)
        );
    }

    @Override
    public List<BibEntry> readCitations(BibEntry entry) {
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
    public void insertReferences(BibEntry entry, List<BibEntry> references) {
        referencesDao.addRelations(
                entry, Objects.requireNonNullElseGet(references, List::of)
        );
    }

    @Override
    public List<BibEntry> readReferences(BibEntry entry) {
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
        this.citationsDao.close();
        this.referencesDao.close();
    }

    public static BibEntryCitationsAndReferencesRepositoryShell of(Path citationsRelationsDirectory,
                                                                   int storeTTL,
                                                                   ImportFormatPreferences importFormatPreferences,
                                                                   FieldPreferences fieldPreferences,
                                                                   BibEntryTypesManager entryTypesManager) {
        Path citationsPath = citationsRelationsDirectory.resolve("%s.mv".formatted(CITATIONS_STORE));
        Path relationsPath = citationsRelationsDirectory.resolve("%s.mv".formatted(REFERENCES_STORE));
        return new BibEntryCitationsAndReferencesRepositoryShell(
                new MVStoreBibEntryRelationRepository(citationsPath, CITATIONS_STORE, storeTTL, entryTypesManager, importFormatPreferences, fieldPreferences),
                new MVStoreBibEntryRelationRepository(relationsPath, REFERENCES_STORE, storeTTL, entryTypesManager, importFormatPreferences, fieldPreferences)
        );
    }
}
