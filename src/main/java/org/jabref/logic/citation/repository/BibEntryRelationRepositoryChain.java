package org.jabref.logic.citation.repository;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public class BibEntryRelationRepositoryChain implements BibEntryRelationRepository {

    private static final BibEntryRelationRepository EMPTY = new BibEntryRelationRepositoryChain(null, null);

    private final BibEntryRelationRepository current;
    private final BibEntryRelationRepository next;

    BibEntryRelationRepositoryChain(BibEntryRelationRepository current, BibEntryRelationRepository next) {
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

    public static BibEntryRelationRepository of(BibEntryRelationRepository... dao) {
        return List.of(dao)
            .reversed()
            .stream()
            .reduce(EMPTY, (acc, current) -> new BibEntryRelationRepositoryChain(current, acc));
    }
}
