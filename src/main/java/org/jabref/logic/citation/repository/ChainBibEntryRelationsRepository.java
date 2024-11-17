package org.jabref.logic.citation.repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.jabref.model.entry.BibEntry;

public class ChainBibEntryRelationsRepository implements BibEntryRelationsRepository {

    private final BibEntryRelationDAO citationsDao;
    private final BibEntryRelationDAO referencesDao;

    public ChainBibEntryRelationsRepository(Path citationsStore, Path relationsStore) {
        this.citationsDao = ChainBibEntryRelationDAO.of(
            LRUCacheBibEntryRelationsDAO.CITATIONS, new MVStoreBibEntryRelationDAO(citationsStore, "citations")
        );
        this.referencesDao = ChainBibEntryRelationDAO.of(
            LRUCacheBibEntryRelationsDAO.REFERENCES, new MVStoreBibEntryRelationDAO(relationsStore, "relations")
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
        return citationsDao.getRelations(entry);
    }

    @Override
    public boolean containsCitations(BibEntry entry) {
        return citationsDao.containsKey(entry);
    }

    @Override
    public void insertReferences(BibEntry entry, List<BibEntry> references) {
        referencesDao.cacheOrMergeRelations(
            entry, Objects.requireNonNullElseGet(references, List::of)
        );
    }

    @Override
    public List<BibEntry> readReferences(BibEntry entry) {
        return referencesDao.getRelations(entry);
    }

    @Override
    public boolean containsReferences(BibEntry entry) {
        return referencesDao.containsKey(entry);
    }
}
