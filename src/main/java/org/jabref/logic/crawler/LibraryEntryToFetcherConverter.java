package org.jabref.logic.crawler;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.UnknownField;

import static org.jabref.model.entry.types.SystematicLiteratureReviewStudyEntryType.LIBRARY_ENTRY;

/**
 * Converts library entries from the given study into their corresponding fetchers.
 */
class LibraryEntryToFetcherConverter {
    private final List<BibEntry> libraryEntries;
    private final ImportFormatPreferences importFormatPreferences;

    public LibraryEntryToFetcherConverter(List<BibEntry> libraryEntries, ImportFormatPreferences importFormatPreferences) {
        this.libraryEntries = libraryEntries;
        this.importFormatPreferences = importFormatPreferences;
    }

    /**
     * Returns a list of instances of all active library fetchers.
     *
     * A fetcher is considered active if there exists an library entry of the library the fetcher is associated with that is enabled.
     *
     * @return Instances of all active fetchers defined in the study definition.
     */
    public List<SearchBasedFetcher> getActiveFetchers() {
        return getFetchersFromLibraryEntries(this.libraryEntries);
    }

    /**
     * Transforms a list of libraryEntries into a list of SearchBasedFetcher instances.
     *
     * @param libraryEntries List of entries
     * @return List of fetcher instances
     */
    private List<SearchBasedFetcher> getFetchersFromLibraryEntries(List<BibEntry> libraryEntries) {
        return libraryEntries.parallelStream()
                             .filter(bibEntry -> bibEntry.getType().getName().equals(LIBRARY_ENTRY.getName()))
                             .map(this::createFetcherFromLibraryEntry)
                             .filter(Objects::nonNull)
                             .collect(Collectors.toList());
    }

    /**
     * Transforms a library entry into a SearchBasedFetcher instance. This only works if the library entry specifies a supported fetcher.
     *
     * @param libraryEntry the entry that will be converted
     * @return An instance of the fetcher defined by the library entry.
     */
    private SearchBasedFetcher createFetcherFromLibraryEntry(BibEntry libraryEntry) {
        Set<SearchBasedFetcher> searchBasedFetchers = WebFetchers.getSearchBasedFetchers(importFormatPreferences);
        String libraryNameFromFetcher = libraryEntry.getField(new UnknownField("name")).orElse("");
        return searchBasedFetchers.stream()
                                  .filter(searchBasedFetcher -> searchBasedFetcher.getName().toLowerCase().equals(libraryNameFromFetcher.toLowerCase()))
                                  .findAny()
                                  .orElse(null);
    }
}
