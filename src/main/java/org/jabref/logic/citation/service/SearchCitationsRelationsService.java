package org.jabref.logic.citation.service;

import java.util.List;

import org.jabref.logic.citation.repository.BibEntryRelationsRepository;
import org.jabref.logic.importer.fetcher.CitationFetcher;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchCitationsRelationsService {

    private static final Logger LOGGER = LoggerFactory
        .getLogger(SearchCitationsRelationsService.class);

    private final CitationFetcher citationFetcher;
    private final BibEntryRelationsRepository relationsRepository;

    public SearchCitationsRelationsService(
        CitationFetcher citationFetcher, BibEntryRelationsRepository repository
    ) {
        this.citationFetcher = citationFetcher;
        this.relationsRepository = repository;
    }

    public List<BibEntry> searchReferences(BibEntry referencer, boolean forceUpdate) {
        if (forceUpdate || !this.relationsRepository.containsReferences(referencer)) {
            try {
                var references = this.citationFetcher.searchCiting(referencer);
                if (!references.isEmpty()) {
                    this.relationsRepository.insertReferences(referencer, references);
                }
            } catch (Exception e) {
                var errMsg = "Error while fetching references for entry %s".formatted(
                    referencer.getTitle()
                );
                LOGGER.error(errMsg);
            }
        }
        return this.relationsRepository.readReferences(referencer);
    }

    public List<BibEntry> searchCitations(BibEntry cited, boolean forceUpdate) {
        if (forceUpdate || !this.relationsRepository.containsCitations(cited)) {
            try {
                var citations = this.citationFetcher.searchCitedBy(cited);
                if (!citations.isEmpty()) {
                    this.relationsRepository.insertCitations(cited, citations);
                }
            } catch (Exception e) {
                var errMsg = "Error while fetching citations for entry %s".formatted(
                    cited.getTitle()
                );
                LOGGER.error(errMsg);
            }
        }
        return this.relationsRepository.readCitations(cited);
    }
}
