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
import org.jabref.logic.importer.fetcher.citation.CitationProviderFactory;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchCitationsRelationsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchCitationsRelationsService.class);

    private CitationFetcher citationFetcher;
    private final BibEntryCitationsAndReferencesRepository relationsRepository;
    @Nullable
    private final ImporterPreferences importerPreferences;
    @Nullable
    private final ImportFormatPreferences importFormatPreferences;
    @Nullable
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    @Nullable
    private final GrobidPreferences grobidPreferences;
    @Nullable
    private final AiService aiService;
    @Nullable
    private String currentProviderName;

    @SuppressWarnings("nullness:argument")
    public SearchCitationsRelationsService(ImporterPreferences importerPreferences,
                                           ImportFormatPreferences importFormatPreferences,
                                           FieldPreferences fieldPreferences,
                                           BibEntryTypesManager entryTypesManager) {
        this(importerPreferences, importFormatPreferences, fieldPreferences, entryTypesManager,
             CitationProviderFactory.getDefaultProvider(), null, null, null);
    }

    public SearchCitationsRelationsService(ImporterPreferences importerPreferences,
                                           ImportFormatPreferences importFormatPreferences,
                                           FieldPreferences fieldPreferences,
                                           BibEntryTypesManager entryTypesManager,
                                           String providerName,
                                           @Nullable CitationKeyPatternPreferences citationKeyPatternPreferences,
                                           @Nullable GrobidPreferences grobidPreferences,
                                           @Nullable AiService aiService) {
        this.importerPreferences = importerPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.grobidPreferences = grobidPreferences;
        this.aiService = aiService;
        this.currentProviderName = providerName;
        
        // Use factory to create the appropriate fetcher
        this.citationFetcher = createFetcher(providerName);
        
        this.relationsRepository = BibEntryCitationsAndReferencesRepositoryShell.of(
                Directories.getCitationsRelationsDirectory(),
                importerPreferences.getCitationsRelationsStoreTTL(),
                importFormatPreferences,
                fieldPreferences,
                entryTypesManager
        );
    }


    private CitationFetcher createFetcher(String providerName) {
        if (citationKeyPatternPreferences != null && grobidPreferences != null && aiService != null) {
            return CitationProviderFactory.getCitationFetcher(
                    providerName,
                    importerPreferences,
                    importFormatPreferences,
                    citationKeyPatternPreferences,
                    grobidPreferences,
                    aiService);
        } else {
            // Fallback to simplified version if dependencies are missing
            return CitationProviderFactory.getCitationFetcher(
                    providerName,
                    importerPreferences);
        }
    }

    /**
     * Updates the citation provider and recreates the fetcher.
     * This allows changing the provider dynamically without recreating the entire service.
     */
    public void updateProvider(String providerName) {
        if (providerName != null && !providerName.equals(currentProviderName)) {
            this.currentProviderName = providerName;
            this.citationFetcher = createFetcher(providerName);
            LOGGER.info("Citation provider updated to: {}", providerName);
        }
    }

    @VisibleForTesting
    SearchCitationsRelationsService(CitationFetcher citationFetcher,
                                    BibEntryCitationsAndReferencesRepository repository
    ) {
        this.citationFetcher = citationFetcher;
        this.relationsRepository = repository;
        // Testing constructor - these fields are not used
        this.importerPreferences = null;
        this.importFormatPreferences = null;
        this.citationKeyPatternPreferences = null;
        this.grobidPreferences = null;
        this.aiService = null;
        this.currentProviderName = null;
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
