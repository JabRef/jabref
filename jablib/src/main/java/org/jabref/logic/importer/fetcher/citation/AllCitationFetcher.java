package org.jabref.logic.importer.fetcher.citation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.database.DatabaseMerger;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class AllCitationFetcher implements CitationFetcher {

    public static final String FETCHER_NAME = "All";

    private static final Logger LOGGER = LoggerFactory.getLogger(AllCitationFetcher.class);

    private final List<CitationFetcher> fetchers;
    private final char keywordSeparator;

    public AllCitationFetcher(ImporterPreferences importerPreferences, ImportFormatPreferences importFormatPreferences, CitationKeyPatternPreferences citationKeyPatternPreferences, GrobidPreferences grobidPreferences, AiService aiService) {
        List<CitationFetcher> providers = new ArrayList<>();
        for (CitationFetcherType type : CitationFetcherType.values()) {
            if (type == CitationFetcherType.ALL) {
                continue;
            }
            providers.add(CitationFetcherType.getCitationFetcher(type, importerPreferences, importFormatPreferences, citationKeyPatternPreferences, grobidPreferences, aiService));
        }
        this.fetchers = List.copyOf(providers);
        this.keywordSeparator = importFormatPreferences.bibEntryPreferences().getKeywordSeparator();
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public List<BibEntry> getReferences(BibEntry entry) throws FetcherException {
        return fetch(f -> f.getReferences(entry));
    }

    @Override
    public List<BibEntry> getCitations(BibEntry entry) throws FetcherException {
        return fetch(f -> f.getCitations(entry));
    }

    @Override
    public Optional<Integer> getCitationCount(BibEntry entry) throws FetcherException {
        boolean anySuccess = false;
        Exception lastException = null;
        Optional<Integer> max = Optional.empty();

        for (CitationFetcher fetcher : fetchers) {
            try {
                Optional<Integer> count = fetcher.getCitationCount(entry);
                anySuccess = true;
                if (count.isPresent()) {
                    max = max.isEmpty() ? count : Optional.of(Math.max(max.get(), count.get()));
                }
            } catch (Exception e) {
                LOGGER.debug("Citation count failed for {}", fetcher.getName(), e);
                lastException = e;
            }
        }

        if (!anySuccess && lastException != null) {
            throw new FetcherException("All citation count providers failed", lastException);
        }

        return max;
    }

    private List<BibEntry> fetch(FetchOperation operation) throws FetcherException {
        BibDatabase target = new BibDatabase();
        DatabaseMerger merger = new DatabaseMerger(keywordSeparator);
        boolean anySuccess = false;
        Exception lastException = null;

        for (CitationFetcher fetcher : fetchers) {
            try {
                List<BibEntry> results = operation.fetch(fetcher);
                BibDatabase other = new BibDatabase();
                other.insertEntries(results);
                merger.merge(target, other);
                anySuccess = true;
            } catch (Exception e) {
                LOGGER.debug("Fetching from {} failed — continuing", fetcher.getName(), e);
                lastException = e;
            }
        }

        if (!anySuccess && lastException != null) {
            throw new FetcherException("All citation providers failed", lastException);
        }

        if (lastException != null) {
            LOGGER.warn("Some citation providers failed, returning partial results");
        }

        return new ArrayList<>(target.getEntries());
    }

    @FunctionalInterface
    private interface FetchOperation {
        List<BibEntry> fetch(CitationFetcher fetcher) throws FetcherException;
    }
}
