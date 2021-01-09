package org.jabref.model.study;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.UnknownField;

/**
 * This class represents a scientific study.
 *
 * This class defines all aspects of a scientific study relevant to the application. It is a proxy for the file based study definition.
 */
public class Study {
    private static final String SEARCH_QUERY_FIELD_NAME = "query";

    private final BibEntry studyEntry;
    private final List<BibEntry> queryEntries;
    private final List<BibEntry> libraryEntries;

    public Study(BibEntry studyEntry, List<BibEntry> queryEntries, List<BibEntry> libraryEntries) {
        this.studyEntry = studyEntry;
        this.queryEntries = queryEntries;
        this.libraryEntries = libraryEntries;
    }

    public List<BibEntry> getAllEntries() {
        List<BibEntry> allEntries = new ArrayList<>();
        allEntries.add(studyEntry);
        allEntries.addAll(queryEntries);
        allEntries.addAll(libraryEntries);
        return allEntries;
    }

    /**
     * Returns all query strings
     *
     * @return List of all queries as Strings.
     */
    public List<String> getSearchQueryStrings() {
        return queryEntries.parallelStream()
                           .map(bibEntry -> bibEntry.getField(new UnknownField(SEARCH_QUERY_FIELD_NAME)))
                           .filter(Optional::isPresent)
                           .map(Optional::get)
                           .collect(Collectors.toList());
    }

    /**
     * This method returns the SearchQuery entries.
     * This is required when the BibKey of the search term entry is required in combination with the search query (e.g.
     * for the creation of the study repository structure).
     */
    public List<BibEntry> getSearchQueryEntries() {
        return queryEntries;
    }

    /**
     * Returns a meta data entry of the first study entry found in the study definition file of the provided type.
     *
     * @param metaDataField The type of requested meta-data
     * @return returns the requested meta data type of the first found study entry
     * @throws IllegalArgumentException If the study file does not contain a study entry.
     */
    public Optional<String> getStudyMetaDataField(StudyMetaDataField metaDataField) throws IllegalArgumentException {
        return studyEntry.getField(metaDataField.toField());
    }

    /**
     * Sets the lastSearchDate field of the study entry
     *
     * @param date date the last time a search was conducted
     */
    public void setLastSearchDate(LocalDate date) {
        studyEntry.setField(StudyMetaDataField.STUDY_LAST_SEARCH.toField(), date.toString());
    }

    /**
     * Extracts all active LibraryEntries from the BibEntries.
     *
     * @return List of BibEntries of type Library
     * @throws IllegalArgumentException If a transformation from Library entry to LibraryDefinition fails
     */
    public List<BibEntry> getActiveLibraryEntries() throws IllegalArgumentException {
        return libraryEntries
                .parallelStream()
                .filter(bibEntry -> {
                    // If enabled is not defined, the fetcher is active.
                    return bibEntry.getField(new UnknownField("enabled"))
                                   .map(enabled -> enabled.equals("true"))
                                   .orElse(true);
                })
                .collect(Collectors.toList());
    }
}

