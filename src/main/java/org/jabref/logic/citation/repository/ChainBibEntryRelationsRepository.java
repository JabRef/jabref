package org.jabref.logic.citation.repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.jabref.model.entry.BibEntry;

public class ChainBibEntryRelationsRepository implements BibEntryRelationsRepository {

    private static final String CITATIONS_STORE = "citations";
    private static final String REFERENCES_STORE = "references";

    private final BibEntryRelationDAO citationsDao;
    private final BibEntryRelationDAO referencesDao;

    public ChainBibEntryRelationsRepository(Path citationsStore, Path relationsStore, int storeTTL) {
        this.citationsDao = BibEntryRelationDAOChain.of(
            LRUCacheBibEntryRelationsDAO.CITATIONS,
            new MVStoreBibEntryRelationDAO(citationsStore, CITATIONS_STORE, storeTTL)
        );
        this.referencesDao = BibEntryRelationDAOChain.of(
            LRUCacheBibEntryRelationsDAO.REFERENCES,
            new MVStoreBibEntryRelationDAO(relationsStore, REFERENCES_STORE, storeTTL)
        );
    }

    @Override
    public void insertCitations(BibEntry entry, List<BibEntry> citations) {
        citationsDao.cacheOrMergeRelations(
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
        return citationsDao.isUpdatable(entry);
    }

    @Override
    public void insertReferences(BibEntry entry, List<BibEntry> references) {
        referencesDao.cacheOrMergeRelations(
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
        return referencesDao.isUpdatable(entry);
    }

    public static ChainBibEntryRelationsRepository of(Path citationsRelationsDirectory, int storeTTL) {
        var citationsPath = citationsRelationsDirectory.resolve("%s.mv".formatted(CITATIONS_STORE));
        var relationsPath = citationsRelationsDirectory.resolve("%s.mv".formatted(REFERENCES_STORE));
        return new ChainBibEntryRelationsRepository(citationsPath, relationsPath, storeTTL);
    }
}
