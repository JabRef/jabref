package org.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    private static final int FETCHER_TIMEOUT = 10;

    private final List<FulltextFetcher> finders = new ArrayList<>();

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
            try {
                WebFetchers.getIdFetcherForIdentifier(DOI.class)
                        .findIdentifier(clonedEntry)
                        .ifPresent(e -> clonedEntry.setField(FieldName.DOI, e.getDOI()));
            } catch (FetcherException e) {
                LOGGER.debug("Failed to find DOI", e);
            }
        }

        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            List<Future<Optional<FetcherResult>>> result = new ArrayList<>();
            try {
                result = executor.invokeAll(getCallables(clonedEntry, finders), FETCHER_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {}

            return result.stream()
                    .map(FulltextFetchers::waitForResult)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(res -> Objects.nonNull(res.source))
                    .sorted(Comparator.comparingInt((FetcherResult res) -> res.trust.getTrustScore()).reversed())
                    .map(res -> res.source)
                    .findFirst();
        } finally {
            shutdownAndAwaitTermination(executor);
        }
    }

    private static Optional<FetcherResult> waitForResult(Future<Optional<FetcherResult>> future) {
        while (true) {
            try {
                return future.get();
            } catch (InterruptedException retry) {

            } catch (ExecutionException | CancellationException e) {
                LOGGER.debug("Fetcher execution failed or was cancelled");
                return Optional.empty();
            }
        }
    }

    private Callable<Optional<FetcherResult>> getCallable(BibEntry entry, FulltextFetcher fetcher) {
            return () -> {
                try {
                    Optional<URL> result = fetcher.findFullText(entry);

                    if (result.isPresent() && new URLDownload(result.get().toString()).isPdf()) {
                        return Optional.of(new FetcherResult(fetcher.getTrustLevel(), result.get()));
                    }
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

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        // Disable new tasks from being submitted
        pool.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks
                pool.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warn("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
