package org.jabref.logic.crawler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.crawler.git.GitHandler;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.study.QueryResult;
import org.jabref.model.study.Study;
import org.jabref.model.util.FileUpdateMonitor;

import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * This class provides a service for SLR support by conducting an automated search and persistance
 * of studies using the queries and E-Libraries specified in the provided study definition file.
 *
 * It composes a StudyRepository for repository management,
 * and a StudyFetcher that manages the crawling over the selected E-Libraries.
 */
public class Crawler {
    private final StudyRepository studyRepository;
    private final StudyFetcher studyFetcher;

    /**
     * Creates a crawler for retrieving studies from E-Libraries
     *
     * @param studyDefinitionFile The path to the study definition file that contains the list of targeted E-Libraries
     *                            and used cross-library queries
     */
    public Crawler(Path studyDefinitionFile, GitHandler gitHandler, FileUpdateMonitor fileUpdateMonitor, ImportFormatPreferences importFormatPreferences, SavePreferences savePreferences, TimestampPreferences timestampPreferences, BibEntryTypesManager bibEntryTypesManager) throws IllegalArgumentException, IOException, ParseException, GitAPIException {
        Path studyRepositoryRoot = studyDefinitionFile.getParent();
        studyRepository = new StudyRepository(studyRepositoryRoot, gitHandler, importFormatPreferences, fileUpdateMonitor, savePreferences, timestampPreferences, bibEntryTypesManager);
        Study study = studyRepository.getStudy();
        LibraryEntryToFetcherConverter libraryEntryToFetcherConverter = new LibraryEntryToFetcherConverter(study.getActiveLibraryEntries(), importFormatPreferences);
        this.studyFetcher = new StudyFetcher(libraryEntryToFetcherConverter.getActiveFetchers(), study.getSearchQueryStrings());
    }

    /**
     * This methods performs the crawling of the active libraries defined in the study definition file.
     * This method also persists the results in the same folder the study definition file is stored in.
     *
     * @throws IOException Thrown if a problem occurred during the persistence of the result.
     */
    public void performCrawl() throws IOException, GitAPIException {
        List<QueryResult> results = studyFetcher.crawl();
        studyRepository.persist(results);
    }
}
