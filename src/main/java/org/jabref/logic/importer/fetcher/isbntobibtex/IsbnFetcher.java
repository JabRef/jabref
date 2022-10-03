package org.jabref.logic.importer.fetcher.isbntobibtex;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.AbstractIsbnFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.OptionalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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
    private final OpenLibraryIsbnFetcher openLibraryIsbnFetcher;
    private final List<AbstractIsbnFetcher> retryIsbnFetcher;

    public IsbnFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
        this.openLibraryIsbnFetcher = new OpenLibraryIsbnFetcher(importFormatPreferences);
        this.retryIsbnFetcher = new ArrayList<>();
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
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        Optional<BibEntry> bibEntry = Optional.empty();

        try {
            identifier = removeNewlinesAndSpacesFromIdentifier(identifier);
            bibEntry = openLibraryIsbnFetcher.performSearchById(identifier);
        } catch (FetcherException ex) {
            LOGGER.debug("Got a fetcher exception for IBSN search", ex);
        } finally {
            while (bibEntry.isEmpty() && retryIsbnFetcher.iterator().hasNext()) {
                AbstractIsbnFetcher fetcher = retryIsbnFetcher.iterator().next();
                LOGGER.debug("No entry found for ISBN=" + identifier + "; trying " + fetcher.getName() + " next.");
                bibEntry = fetcher.performSearchById(identifier);
            }
        }

        if (bibEntry.isEmpty()) {
            LOGGER.debug("Could not found a entry for ISBN=" + identifier);
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
        if (retryFetcher == null) {
            throw new IllegalArgumentException("Please provide a valid isbn fetcher.");
        }
        retryIsbnFetcher.add(retryFetcher);
        return this;
    }

    private String removeNewlinesAndSpacesFromIdentifier(String identifier) {
        return NEWLINE_SPACE_PATTERN.matcher(identifier).replaceAll("");
    }
}
