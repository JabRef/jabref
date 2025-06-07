package org.jabref.logic.citation;

import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citation.repository.BibEntryCitationsAndReferencesRepository;
import org.jabref.logic.citation.repository.BibEntryCitationsAndReferencesRepositoryShell;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.util.Directories;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchCitationsRelationsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchCitationsRelationsService.class);

    private final CitationFetcher citationFetcher;
    private final BibEntryCitationsAndReferencesRepository relationsRepository;

    public SearchCitationsRelationsService(ImporterPreferences importerPreferences,
                                           ImportFormatPreferences importFormatPreferences,
                                           FieldPreferences fieldPreferences,
                                           BibEntryTypesManager entryTypesManager) {
        this.citationFetcher = new SemanticScholarCitationFetcher(importerPreferences);
        this.relationsRepository = BibEntryCitationsAndReferencesRepositoryShell.of(
                Directories.getCitationsRelationsDirectory(),
                importerPreferences.getCitationsRelationsStoreTTL(),
                importFormatPreferences,
                fieldPreferences,
                entryTypesManager
        );
    }

    /**
     * @implNote Typically, this would be a Shim in JavaFX
     */
    @VisibleForTesting
    public SearchCitationsRelationsService(CitationFetcher citationFetcher,
                                           BibEntryCitationsAndReferencesRepository repository
    ) {
        this.citationFetcher = citationFetcher;
        this.relationsRepository = repository;
    }

    public List<BibEntry> searchReferences(BibEntry referenced) {
        boolean isFetchingAllowed = relationsRepository.isReferencesUpdatable(referenced)
            || !relationsRepository.containsReferences(referenced);
        if (isFetchingAllowed) {
            try {
                List<BibEntry> referencedBy = citationFetcher.searchCiting(referenced);
                relationsRepository.insertReferences(referenced, referencedBy);
            } catch (FetcherException e) {
                LOGGER.error("Error while fetching references for entry {}", referenced.getTitle(), e);
            }
        }
        return relationsRepository.readReferences(referenced);
    }

    /**
     * If the store was empty and nothing was fetch in any case (empty fetch, or error) then yes => empty list
     * If the store was not empty and nothing was fetched after a successful fetch => the store will be erased and the returned collection will be empty
     * If the store was not empty and an error occurs while fetching => will return the content of the store
     */
    public List<BibEntry> searchCitations(BibEntry cited) {
        boolean isFetchingAllowed = relationsRepository.isCitationsUpdatable(cited)
            || !relationsRepository.containsCitations(cited);
        if (isFetchingAllowed) {
            try {
                List<BibEntry> citedBy = citationFetcher.searchCitedBy(cited);
                relationsRepository.insertCitations(cited, citedBy);
            } catch (FetcherException e) {
                LOGGER.error("Error while fetching citations for entry {}", cited.getTitle(), e);
            }
        }
        return relationsRepository.readCitations(cited);
    }

    public void close() {
        relationsRepository.close();
    }
}
