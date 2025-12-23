package org.jabref.logic.citation;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citation.repository.BibEntryCitationsAndReferencesRepository;
import org.jabref.logic.citation.repository.BibEntryCitationsAndReferencesRepositoryShell;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherFactory;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchCitationsRelationsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchCitationsRelationsService.class);

    private CitationFetcher citationFetcher;
    private final BibEntryCitationsAndReferencesRepository relationsRepository;

    public SearchCitationsRelationsService(ImporterPreferences importerPreferences,
                                           ImportFormatPreferences importFormatPreferences,
                                           FieldPreferences fieldPreferences,
                                           ObjectProperty<CitationFetcherType> citationFetcherTypeProperty,
                                           CitationKeyPatternPreferences citationKeyPatternPreferences,
                                           GrobidPreferences grobidPreferences,
                                           AiService aiService,
                                           BibEntryTypesManager entryTypesManager) {

        this.citationFetcher = CitationFetcherFactory.INSTANCE.getCitationFetcher(
                citationFetcherTypeProperty.get().getFetcherName(),
                importerPreferences,
                importFormatPreferences,
                citationKeyPatternPreferences,
                grobidPreferences,
                aiService);

        citationFetcherTypeProperty.addListener((_, _, newValue) -> {
            this.citationFetcher = CitationFetcherFactory.INSTANCE.getCitationFetcher(
                    newValue.getFetcherName(),
                    importerPreferences,
                    importFormatPreferences,
                    citationKeyPatternPreferences,
                    grobidPreferences,
                    aiService);
        });

        this.relationsRepository = BibEntryCitationsAndReferencesRepositoryShell.of(
                Directories.getCitationsRelationsDirectory(),
                importerPreferences.getCitationsRelationsStoreTTL(),
                importFormatPreferences,
                fieldPreferences,
                entryTypesManager
        );
    }

    @VisibleForTesting
    SearchCitationsRelationsService(CitationFetcher citationFetcher,
                                    BibEntryCitationsAndReferencesRepository repository
    ) {
        this.citationFetcher = citationFetcher;
        this.relationsRepository = repository;
    }

    public List<BibEntry> searchCites(BibEntry referencing) throws FetcherException {
        boolean isFetchingAllowed =
                !relationsRepository.containsReferences(referencing) ||
                        relationsRepository.isReferencesUpdatable(referencing);
        if (isFetchingAllowed) {
            List<BibEntry> referencedBy = citationFetcher.getReferences(referencing);
            relationsRepository.insertReferences(referencing, referencedBy);
        }
        return relationsRepository.readReferences(referencing);
    }

    /**
     * If the store was empty and nothing was fetch in any case (empty fetch, or error) then yes => empty list
     * If the store was not empty and nothing was fetched after a successful fetch => the store will be erased and the returned collection will be empty
     * If the store was not empty and an error occurs while fetching => will return the content of the store
     */
    public List<BibEntry> searchCitedBy(BibEntry cited) throws FetcherException {
        boolean isFetchingAllowed =
                !relationsRepository.containsCitations(cited) ||
                        relationsRepository.isCitationsUpdatable(cited);
        if (isFetchingAllowed) {
            List<BibEntry> citedBy = citationFetcher.getCitations(cited);
            relationsRepository.insertCitations(cited, citedBy);
        }
        return relationsRepository.readCitations(cited);
    }

    public int getCitationCount(BibEntry citationCounted, Optional<String> actualFieldValue) throws FetcherException {
        boolean isFetchingAllowed = actualFieldValue.isEmpty() ||
                relationsRepository.isCitationsUpdatable(citationCounted);
        if (isFetchingAllowed) {
            Optional<Integer> citationCountResult = citationFetcher.getCitationCount(citationCounted);
            return citationCountResult.orElse(0);
        }
        assert actualFieldValue.isPresent();
        return Integer.parseInt(actualFieldValue.get());
    }

    public void close() {
        relationsRepository.close();
    }
}
