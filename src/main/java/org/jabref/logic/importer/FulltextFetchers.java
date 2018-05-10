package org.jabref.logic.importer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jabref.JabRefExecutorService;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for trying to resolve URLs to full-text PDF for articles.
 */
public class FulltextFetchers {
    private static final Logger LOGGER = LoggerFactory.getLogger(FulltextFetchers.class);

    // Timeout in seconds
    private static final int FETCHER_TIMEOUT = 10;

    private final List<FulltextFetcher> finders = new ArrayList<>();

    private final Predicate<String> isPDF = url -> {
        try {
            return new URLDownload(url).isPdf();
        } catch (MalformedURLException e) {
            LOGGER.warn("URL returned by fulltext fetcher is invalid");
        }
        return false;
    };

    public FulltextFetchers(ImportFormatPreferences importFormatPreferences) {
        this(WebFetchers.getFullTextFetchers(importFormatPreferences));
    }

    FulltextFetchers(List<FulltextFetcher> fetcher) {
        finders.addAll(fetcher);
    }

    public Optional<URL> findFullTextPDF(BibEntry entry) {
        // for accuracy, fetch DOI first but do not modify entry
        BibEntry clonedEntry = (BibEntry) entry.clone();
        Optional<DOI> doi = clonedEntry.getField(FieldName.DOI).flatMap(DOI::parse);

        if (!doi.isPresent()) {
            findDoiForEntry(clonedEntry);
        }

        List<Future<Optional<FetcherResult>>> result = new ArrayList<>();
        result = JabRefExecutorService.INSTANCE.executeAll(getCallables(clonedEntry, finders), FETCHER_TIMEOUT, TimeUnit.SECONDS);

        return result.stream()
                .map(FulltextFetchers::getResults)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(res -> Objects.nonNull(res.getSource()))
                .sorted(Comparator.comparingInt((FetcherResult res) -> res.getTrust().getTrustScore()).reversed())
                .map(res -> res.getSource())
                .findFirst();
    }

    private void findDoiForEntry(BibEntry clonedEntry) {
        try {
            WebFetchers.getIdFetcherForIdentifier(DOI.class)
                    .findIdentifier(clonedEntry)
                    .ifPresent(e -> clonedEntry.setField(FieldName.DOI, e.getDOI()));
        } catch (FetcherException e) {
            LOGGER.debug("Failed to find DOI", e);
        }
    }

    private static Optional<FetcherResult> getResults(Future<Optional<FetcherResult>> future) {
        try {
            return future.get();
        } catch (InterruptedException ignore) {

        } catch (ExecutionException | CancellationException e) {
            LOGGER.debug("Fetcher execution failed or was cancelled");
        }
        return Optional.empty();
    }

    private Callable<Optional<FetcherResult>> getCallable(BibEntry entry, FulltextFetcher fetcher) {
        return () -> {
            try {
                return fetcher.findFullText(entry)
                        .filter(url -> isPDF.test(url.toString()))
                        .map(url -> new FetcherResult(fetcher.getTrustLevel(), url));
            } catch (IOException | FetcherException e) {
                LOGGER.debug("Failed to find fulltext PDF at given URL", e);
            }
            return Optional.empty();
        };
    }

    private List<Callable<Optional<FetcherResult>>> getCallables(BibEntry entry, List<FulltextFetcher> fetchers) {
        return fetchers.stream()
                .map(f -> getCallable(entry, f))
                .collect(Collectors.toList());
    }
}
