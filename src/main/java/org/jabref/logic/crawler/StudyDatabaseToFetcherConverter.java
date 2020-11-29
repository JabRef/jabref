package org.jabref.logic.crawler;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.model.study.StudyDatabase;

/**
 * Converts library entries from the given study into their corresponding fetchers.
 */
class StudyDatabaseToFetcherConverter {
    private final List<StudyDatabase> libraryEntries;
    private final ImportFormatPreferences importFormatPreferences;

    public StudyDatabaseToFetcherConverter(List<StudyDatabase> libraryEntries, ImportFormatPreferences importFormatPreferences) {
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
    private List<SearchBasedFetcher> getFetchersFromLibraryEntries(List<StudyDatabase> libraryEntries) {
        return libraryEntries.parallelStream()
                             .map(this::createFetcherFromLibraryEntry)
                             .filter(Objects::nonNull)
                             .collect(Collectors.toList());
    }

    /**
     * Transforms a library entry into a SearchBasedFetcher instance. This only works if the library entry specifies a supported fetcher.
     *
     * @param studyDatabase the entry that will be converted
     * @return An instance of the fetcher defined by the library entry.
     */
    private SearchBasedFetcher createFetcherFromLibraryEntry(StudyDatabase studyDatabase) {
        Set<SearchBasedFetcher> searchBasedFetchers = WebFetchers.getSearchBasedFetchers(importFormatPreferences);
        String libraryNameFromFetcher = studyDatabase.getName();
        return searchBasedFetchers.stream()
                                  .filter(searchBasedFetcher -> searchBasedFetcher.getName().toLowerCase().equals(libraryNameFromFetcher.toLowerCase()))
                                  .findAny()
                                  .orElse(null);
    }
}
