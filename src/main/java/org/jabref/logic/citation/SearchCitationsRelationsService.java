package org.jabref.logic.citation;

import java.util.List;

import org.jabref.logic.citation.repository.BibEntryRelationsRepository;
import org.jabref.logic.citation.repository.BibEntryRelationsRepositoryChain;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.CitationFetcher;
import org.jabref.logic.importer.fetcher.SemanticScholarCitationFetcher;
import org.jabref.logic.util.Directories;
import org.jabref.model.entry.BibEntry;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchCitationsRelationsService {

    private static final Logger LOGGER = LoggerFactory
        .getLogger(SearchCitationsRelationsService.class);

    private final CitationFetcher citationFetcher;
    private final BibEntryRelationsRepository relationsRepository;

    public SearchCitationsRelationsService(ImporterPreferences importerPreferences) {
        this.citationFetcher = new SemanticScholarCitationFetcher(importerPreferences);
        this.relationsRepository = BibEntryRelationsRepositoryChain.of(
            Directories.getCitationsRelationsDirectory(),
            importerPreferences.getCitationsRelationsStoreTTL()
        );
    }

    /**
     * @implNote Typically, this would be a Shim in JavaFX
     */
    @VisibleForTesting
    public SearchCitationsRelationsService(
            CitationFetcher citationFetcher, BibEntryRelationsRepository repository
    ) {
        this.citationFetcher = citationFetcher;
        this.relationsRepository = repository;
    }

    public List<BibEntry> searchReferences(BibEntry referencer) {
        boolean isFetchingAllowed = this.relationsRepository.isReferencesUpdatable(referencer)
            || !this.relationsRepository.containsReferences(referencer);
        if (isFetchingAllowed) {
            try {
                var references = this.citationFetcher.searchCiting(referencer);
                this.relationsRepository.insertReferences(referencer, references);
            } catch (FetcherException e) {
                LOGGER.error("Error while fetching references for entry {}", referencer.getTitle(), e);
            }
        }
        return this.relationsRepository.readReferences(referencer);
    }

    /**
     * If the store was empty and nothing was fetch in any case (empty fetch, or error) then yes => empty list
     * If the store was not empty and nothing was fetched after a successful fetch => the store will be erased and the returned collection will be empty
     * If the store was not empty and an error occurs while fetching => will return the content of the store
     */
    public List<BibEntry> searchCitations(BibEntry cited) {
        boolean isFetchingAllowed = this.relationsRepository.isCitationsUpdatable(cited)
            || !this.relationsRepository.containsCitations(cited);
        if (isFetchingAllowed) {
            try {
                var citations = this.citationFetcher.searchCitedBy(cited);
                this.relationsRepository.insertCitations(cited, citations);
            } catch (FetcherException e) {
                LOGGER.error("Error while fetching citations for entry {}", cited.getTitle(), e);
            }
        }
        return this.relationsRepository.readCitations(cited);
    }
}
