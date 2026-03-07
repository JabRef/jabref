package org.jabref.logic.importer.fetcher.citation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.OpenAlex;
import org.jabref.logic.importer.fetcher.citation.crossref.CrossRefCitationFetcher;
import org.jabref.logic.importer.fetcher.citation.opencitations.OpenCitationsFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class AllCitationFetcher implements CitationFetcher {

    public static final String FETCHER_NAME = "All";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AllCitationFetcher.class);

    private final List<CitationFetcher> fetchers;
    private final DuplicateCheck duplicateCheck;

    public AllCitationFetcher(
            ImporterPreferences importerPreferences,
            ImportFormatPreferences importFormatPreferences,
            CitationKeyPatternPreferences citationKeyPatternPreferences,
            GrobidPreferences grobidPreferences,
            AiService aiService) {

        this.fetchers = List.of(
                new CrossRefCitationFetcher(
                        importerPreferences,
                        importFormatPreferences,
                        citationKeyPatternPreferences,
                        grobidPreferences,
                        aiService),
                new OpenAlex(importerPreferences),
                new OpenCitationsFetcher(importerPreferences),
                new SemanticScholarCitationFetcher(importerPreferences)
        );
        this.duplicateCheck = new DuplicateCheck(new BibEntryTypesManager());
    }

    // Test constructor
    AllCitationFetcher(List<CitationFetcher> fetchers) {
        this.fetchers = fetchers;
        this.duplicateCheck = new DuplicateCheck(new BibEntryTypesManager());
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public List<BibEntry> getReferences(BibEntry entry) throws FetcherException {
        return deduplicate(fetch(f -> f.getReferences(entry)));
    }

    @Override
    public List<BibEntry> getCitations(BibEntry entry) throws FetcherException {
        return deduplicate(fetch(f -> f.getCitations(entry)));
    }

    @Override
    public Optional<Integer> getCitationCount(BibEntry entry) {
        return fetchers.stream()
                       .map(fetcher -> {
                           try {
                               return fetcher.getCitationCount(entry);
                           } catch (FetcherException e) {
                               LOGGER.debug("Citation count failed for {}",
                                       fetcher.getName(), e);
                               return Optional.<Integer>empty();
                           }
                       })
                       .flatMap(Optional::stream)
                       .max(Integer::compareTo);
    }

    private List<BibEntry> fetch(FetchOperation operation) {
        List<BibEntry> result = new ArrayList<>();
        for (CitationFetcher fetcher : fetchers) {
            try {
                result.addAll(operation.fetch(fetcher));
            } catch (FetcherException e) {
                LOGGER.debug("Fetching from {} failed — continuing",
                        fetcher.getName(), e);
            }
        }
        return result;
    }

    private List<BibEntry> deduplicate(List<BibEntry> entries) {
        BibDatabase temp = new BibDatabase();
        List<BibEntry> unique = new ArrayList<>();
        for (BibEntry entry : entries) {
            if (duplicateCheck
                    .containsDuplicate(temp, entry, BibDatabaseMode.BIBTEX)
                    .isEmpty()) {
                temp.insertEntry(entry);
                unique.add(entry);
            }
        }
        return unique;
    }

    @FunctionalInterface
    private interface FetchOperation {
        List<BibEntry> fetch(CitationFetcher fetcher) throws FetcherException;
    }
}
