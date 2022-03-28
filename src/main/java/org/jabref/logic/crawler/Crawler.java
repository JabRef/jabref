package org.jabref.logic.crawler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.git.SlrGitHandler;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.study.QueryResult;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;

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
     * @param studyRepositoryRoot The path to the study repository
     */
    public Crawler(Path studyRepositoryRoot, SlrGitHandler gitHandler, GeneralPreferences generalPreferences, ImportFormatPreferences importFormatPreferences, SavePreferences savePreferences, BibEntryTypesManager bibEntryTypesManager, FileUpdateMonitor fileUpdateMonitor) throws IllegalArgumentException, IOException, ParseException {
        studyRepository = new StudyRepository(studyRepositoryRoot, gitHandler, generalPreferences, importFormatPreferences, fileUpdateMonitor, savePreferences, bibEntryTypesManager);
        StudyDatabaseToFetcherConverter studyDatabaseToFetcherConverter = new StudyDatabaseToFetcherConverter(studyRepository.getActiveLibraryEntries(), importFormatPreferences);
        this.studyFetcher = new StudyFetcher(studyDatabaseToFetcherConverter.getActiveFetchers(), studyRepository.getSearchQueryStrings());
    }

    /**
     * This methods performs the crawling of the active libraries defined in the study definition file.
     * This method also persists the results in the same folder the study definition file is stored in.
     *
     * The whole process works as follows:
     * <ol>
     *     <li>Then the search is executed</li>
     *     <li>The repository changes to the search branch</li>
     *     <li>Afterwards, the results are persisted on the search branch.</li>
     *     <li>Finally, the changes are merged into the work branch</li>
     * </ol>
     *
     * @throws IOException Thrown if a problem occurred during the persistence of the result.
     */
    public void performCrawl() throws IOException, GitAPIException, SaveException {
        List<QueryResult> results = studyFetcher.crawl();
        studyRepository.persist(results);
    }
}
