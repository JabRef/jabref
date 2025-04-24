package org.jabref.logic.importer.fetcher.isbntobibtex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.AbstractIsbnFetcher;
import org.jabref.logic.importer.fetcher.GvkFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.util.OptionalUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetcher to generate the Bibtex entry from an ISBN.
 * The default fetcher is the {@link OpenLibraryIsbnFetcher}.
 * If the entry is not found in the {@link OpenLibraryIsbnFetcher}.
 * Alternative fetcher can be specified with the {@link IsbnFetcher#addRetryFetcher(AbstractIsbnFetcher)} method.
 */
public class IsbnFetcher implements EntryBasedFetcher, IdBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IsbnFetcher.class);
    private static final Pattern NEWLINE_SPACE_PATTERN = Pattern.compile("\\n|\\r\\n|\\s");
    protected final ImportFormatPreferences importFormatPreferences;
    private final List<AbstractIsbnFetcher> retryIsbnFetcher;
    private final GvkFetcher gvkIsbnFetcher;

    public IsbnFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
        OpenLibraryIsbnFetcher openLibraryIsbnFetcher = new OpenLibraryIsbnFetcher(importFormatPreferences);
        this.gvkIsbnFetcher = new GvkFetcher(importFormatPreferences);
        this.retryIsbnFetcher = new ArrayList<>();
        this.addRetryFetcher(openLibraryIsbnFetcher);
    }

    @Override
    public String getName() {
        return "ISBN";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ISBN);
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<BibEntry> bibEntry = Optional.empty();

        try {
            identifier = removeNewlinesAndSpacesFromIdentifier(identifier);
            Optional<ISBN> isbn = ISBN.parse(identifier);
            if (isbn.isPresent()) {
                bibEntry = gvkIsbnFetcher.performSearchById(isbn.get().asString());
            }
        } catch (FetcherException ex) {
            LOGGER.debug("Got a fetcher exception for IBSN search", ex);
            if (retryIsbnFetcher.isEmpty()) {
                throw ex;
            }
        } finally {
            // do not move the iterator in the loop as this would always return a new one and thus create and endless loop
            Iterator<AbstractIsbnFetcher> iterator = retryIsbnFetcher.iterator();
            while (bibEntry.isEmpty() && iterator.hasNext()) {
                LOGGER.debug("Trying using the alternate ISBN fetchers to find an entry.");

                AbstractIsbnFetcher fetcher = iterator.next();
                LOGGER.debug("No entry found for ISBN {}; trying {} next.", identifier, fetcher.getName());
                bibEntry = fetcher.performSearchById(identifier);
            }
        }

        if (bibEntry.isEmpty()) {
            LOGGER.debug("Could not found a entry for ISBN {}", identifier);
        }

        return bibEntry;
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> isbn = entry.getField(StandardField.ISBN);
        if (isbn.isPresent()) {
            return OptionalUtil.toList(performSearchById(isbn.get()));
        } else {
            return Collections.emptyList();
        }
    }

    public IsbnFetcher addRetryFetcher(AbstractIsbnFetcher retryFetcher) {
        Objects.requireNonNull(retryFetcher, "Please provide a valid isbn fetcher.");
        retryIsbnFetcher.add(retryFetcher);
        return this;
    }

    private String removeNewlinesAndSpacesFromIdentifier(String identifier) {
        return NEWLINE_SPACE_PATTERN.matcher(identifier).replaceAll("");
    }
}
