package org.jabref.logic.citation;

import java.util.List;
import java.util.Optional;

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

    // Store dependencies so we can recreate the citationFetcher at runtime
    private final ImporterPreferences importerPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final FieldPreferences fieldPreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final GrobidPreferences grobidPreferences;
    private final AiService aiService;
    private final BibEntryTypesManager entryTypesManager;

    public SearchCitationsRelationsService(ImporterPreferences importerPreferences,
                                           ImportFormatPreferences importFormatPreferences,
                                           FieldPreferences fieldPreferences,
                                           String citationFetcherName,
                                           CitationKeyPatternPreferences citationKeyPatternPreferences,
                                           GrobidPreferences grobidPreferences,
                                           AiService aiService,
                                           BibEntryTypesManager entryTypesManager) {
        this.importerPreferences = importerPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.fieldPreferences = fieldPreferences;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.grobidPreferences = grobidPreferences;
        this.aiService = aiService;
        this.entryTypesManager = entryTypesManager;

        this.citationFetcher = CitationFetcherFactory.INSTANCE.getCitationFetcher(citationFetcherName, importerPreferences, importFormatPreferences,
                citationKeyPatternPreferences, grobidPreferences, aiService);
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

        // For the testing constructor, set dependencies to null
        this.importerPreferences = null;
        this.importFormatPreferences = null;
        this.fieldPreferences = null;
        this.citationKeyPatternPreferences = null;
        this.grobidPreferences = null;
        this.aiService = null;
        this.entryTypesManager = null;
    }

    /**
     * Allows switching the underlying citation fetcher at runtime. This will recreate the fetcher using the
     * same preferences that were provided when this service was constructed. If this service was created via the
     * testing constructor, this method is a no-op.
     */
    public void setCitationFetcherName(String citationFetcherName) {
        if (importerPreferences == null) {
            return;
        }
        this.citationFetcher = CitationFetcherFactory.INSTANCE.getCitationFetcher(citationFetcherName,
                importerPreferences, importFormatPreferences, citationKeyPatternPreferences, grobidPreferences, aiService);
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
