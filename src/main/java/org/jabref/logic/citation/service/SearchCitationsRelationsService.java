package org.jabref.logic.citation.service;

import java.util.List;
import org.jabref.logic.citation.repository.BibEntryRelationsRepository;
import org.jabref.model.entry.BibEntry;

public class SearchCitationsRelationsService {

    BibEntryRelationsRepository relationsRepository;

    public SearchCitationsRelationsService(BibEntryRelationsRepository repository) {
        this.relationsRepository = repository;
    }

    public List<BibEntry> searchReferences(BibEntry referencer) {
        return this.relationsRepository.readReferences(referencer);
    }

    public List<BibEntry> searchReferences(BibEntry referencer, boolean forceUpdate) {
        if (forceUpdate) {
            this.relationsRepository.forceRefreshReferences(referencer);
        }
        return this.searchReferences(referencer);
    }

    public List<BibEntry> searchCitations(BibEntry cited) {
        return this.relationsRepository.readCitations(cited);
    }

    public List<BibEntry> searchCitations(BibEntry cited, boolean forceUpdate) {
        if (forceUpdate) {
            this.relationsRepository.forceRefreshCitations(cited);
        }
        return this.searchCitations(cited);
    }
}
