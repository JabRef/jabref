package org.jabref.logic.citation.repository;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public class BibEntryRelationDAOChain implements BibEntryRelationDAO {

    private static final BibEntryRelationDAO EMPTY = new BibEntryRelationDAOChain(null, null);

    private final BibEntryRelationDAO current;
    private final BibEntryRelationDAO next;

    BibEntryRelationDAOChain(BibEntryRelationDAO current, BibEntryRelationDAO next) {
        this.current = current;
        this.next = next;
    }

    @Override
    public List<BibEntry> getRelations(BibEntry entry) {
        if (this.current.containsKey(entry)) {
            return this.current.getRelations(entry);
        }
        if (this.next == EMPTY) {
            return List.of();
        }
        var relations = this.next.getRelations(entry);
        this.current.cacheOrMergeRelations(entry, relations);
        // Makes sure to obtain a copy and not a direct reference to what was inserted
        return this.current.getRelations(entry);
    }

    @Override
    public void cacheOrMergeRelations(BibEntry entry, List<BibEntry> relations) {
        if (this.next != EMPTY) {
            this.next.cacheOrMergeRelations(entry, relations);
        }
        this.current.cacheOrMergeRelations(entry, relations);
    }

    @Override
    public boolean containsKey(BibEntry entry) {
        return this.current.containsKey(entry)
            || (this.next != EMPTY && this.next.containsKey(entry));
    }

    @Override
    public boolean isUpdatable(BibEntry entry) {
        return this.current.isUpdatable(entry)
            && (this.next == EMPTY || this.next.isUpdatable(entry));
    }

    public static BibEntryRelationDAO of(BibEntryRelationDAO... dao) {
        return List.of(dao)
            .reversed()
            .stream()
            .reduce(EMPTY, (acc, current) -> new BibEntryRelationDAOChain(current, acc));
    }
}
