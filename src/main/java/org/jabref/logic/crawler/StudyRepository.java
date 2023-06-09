package org.jabref.logic.crawler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.database.DatabaseMerger;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SaveConfiguration;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.git.SlrGitHandler;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileNameCleaner;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.study.FetchResult;
import org.jabref.model.study.QueryResult;
import org.jabref.model.study.Study;
import org.jabref.model.study.StudyDatabase;
import org.jabref.model.study.StudyQuery;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages all aspects of the study process related to the repository.
 *
 * It includes the parsing of the study definition file (study.bib) into a Study instance,
 * the structured persistence of the crawling results for the study within the file based repository,
 * as well as the sharing, and versioning of results using git.
 */
public class StudyRepository {
    // Tests work with study.yml
    public static final String STUDY_DEFINITION_FILE_NAME = "study.yml";

    private static final Logger LOGGER = LoggerFactory.getLogger(StudyRepository.class);

    private static final Pattern MATCH_COLON = Pattern.compile(":");
    private static final Pattern MATCH_ILLEGAL_CHARACTERS = Pattern.compile("[^A-Za-z0-9_.\\s=-]");

    // Currently we make assumptions about the configuration: the remotes, work and search branch names
    private static final String REMOTE = "origin";
    private static final String WORK_BRANCH = "work";
    private static final String SEARCH_BRANCH = "search";

    private final Path repositoryPath;
    private final Path studyDefinitionFile;
    private final SlrGitHandler gitHandler;
    private final Study study;
    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager bibEntryTypesManager;

    /**
     * Creates a study repository.
     *
     * @param pathToRepository Where the repository root is located.
     * @param gitHandler       The git handler that manages any interaction with the remote repository
     * @throws IllegalArgumentException If the repository root directory does not exist, or the root directory does not
     *                                  contain the study definition file.
     * @throws IOException              Thrown if the given repository does not exists, or the study definition file
     *                                  does not exist
     */
    public StudyRepository(Path pathToRepository,
                           SlrGitHandler gitHandler,
                           PreferencesService preferencesService,
                           FileUpdateMonitor fileUpdateMonitor,
                           BibEntryTypesManager bibEntryTypesManager) throws IOException {
        this.repositoryPath = pathToRepository;
        this.gitHandler = gitHandler;
        this.preferencesService = preferencesService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.studyDefinitionFile = Path.of(repositoryPath.toString(), STUDY_DEFINITION_FILE_NAME);
        this.bibEntryTypesManager = bibEntryTypesManager;

        if (Files.notExists(repositoryPath)) {
            throw new IOException("The given repository does not exists.");
        }
        try {
            gitHandler.createCommitOnCurrentBranch("Save changes before searching.", false);
            gitHandler.checkoutBranch(WORK_BRANCH);
            updateWorkAndSearchBranch();
        } catch (GitAPIException e) {
            LOGGER.error("Could not checkout work branch");
        }
        if (Files.notExists(studyDefinitionFile)) {
            throw new IOException("The study definition file does not exist in the given repository.");
        }
        study = parseStudyFile();
        try {
            final String updateRepositoryStructureMessage = "Update repository structure";

            // Update repository structure on work branch in case of changes
            setUpRepositoryStructureForQueriesAndFetchers();
            gitHandler.createCommitOnCurrentBranch(updateRepositoryStructureMessage, false);

            gitHandler.checkoutBranch(SEARCH_BRANCH);
            // If study definition does not exist on this branch or was changed on work branch, copy it from work
            boolean studyDefinitionDoesNotExistOrChanged = !(Files.exists(studyDefinitionFile) && new StudyYamlParser().parseStudyYamlFile(studyDefinitionFile).equals(study));
            if (studyDefinitionDoesNotExistOrChanged) {
                new StudyYamlParser().writeStudyYamlFile(study, studyDefinitionFile);
            }
            setUpRepositoryStructureForQueriesAndFetchers();
            gitHandler.createCommitOnCurrentBranch(updateRepositoryStructureMessage, false);
        } catch (GitAPIException e) {
            LOGGER.error("Could not checkout search branch.");
        }
        try {
            gitHandler.checkoutBranch(WORK_BRANCH);
        } catch (GitAPIException e) {
            LOGGER.error("Could not checkout work branch");
        }
    }

    /**
     * Returns entries stored in the repository for a certain query and fetcher
     */
    public BibDatabaseContext getFetcherResultEntries(String query, String fetcherName) throws IOException {
        if (Files.exists(getPathToFetcherResultFile(query, fetcherName))) {
            return OpenDatabase.loadDatabase(getPathToFetcherResultFile(query, fetcherName),
                    preferencesService.getImportFormatPreferences(),
                    fileUpdateMonitor).getDatabaseContext();
        }
        return new BibDatabaseContext();
    }

    /**
     * Returns the merged entries stored in the repository for a certain query
     */
    public BibDatabaseContext getQueryResultEntries(String query) throws IOException {
        if (Files.exists(getPathToQueryResultFile(query))) {
            return OpenDatabase.loadDatabase(getPathToQueryResultFile(query),
                    preferencesService.getImportFormatPreferences(),
                    fileUpdateMonitor).getDatabaseContext();
        }
        return new BibDatabaseContext();
    }

    /**
     * Returns the merged entries stored in the repository for all queries
     */
    public BibDatabaseContext getStudyResultEntries() throws IOException {
        if (Files.exists(getPathToStudyResultFile())) {
            return OpenDatabase.loadDatabase(getPathToStudyResultFile(),
                    preferencesService.getImportFormatPreferences(),
                    fileUpdateMonitor).getDatabaseContext();
        }
        return new BibDatabaseContext();
    }

    /**
     * The study definition file contains all the definitions of a study. This method extracts this study from the yaml study definition file
     *
     * @return Returns the BibEntries parsed from the study definition file.
     * @throws IOException Problem opening the input stream.
     */
    private Study parseStudyFile() throws IOException {
        return new StudyYamlParser().parseStudyYamlFile(studyDefinitionFile);
    }

    /**
     * Returns all query strings of the study definition
     *
     * @return List of all queries as Strings.
     */
    public List<String> getSearchQueryStrings() {
        return study.getQueries()
                    .parallelStream()
                    .map(StudyQuery::getQuery)
                    .collect(Collectors.toList());
    }

    /**
     * Extracts all active fetchers from the library entries.
     *
     * @return List of BibEntries of type Library
     * @throws IllegalArgumentException If a transformation from Library entry to LibraryDefinition fails
     */
    public List<StudyDatabase> getActiveLibraryEntries() throws IllegalArgumentException {
        return study.getDatabases()
                    .parallelStream()
                    .filter(StudyDatabase::isEnabled)
                    .collect(Collectors.toList());
    }

    public Study getStudy() {
        return study;
    }

    /**
     * Persists the result locally and remotely by following the steps:
     * Precondition: Currently checking out work branch
     * <ol>
     *     <li>Update the work and search branch</li>
     *     <li>Persist the results on the search branch</li>
     *     <li>Manually patch the diff of the search branch onto the work branch (as the merging will not work in
     *     certain cases without a conflict as it is context sensitive. But for this use case we do not need it to be
     *     context sensitive. So we can just prepend the patch without checking the "context" lines.</li>
     *     <li>Update the remote tracking branches of the work and search branch</li>
     * </ol>
     */
    public void persist(List<QueryResult> crawlResults) throws IOException, GitAPIException, SaveException {
        updateWorkAndSearchBranch();

        gitHandler.checkoutBranch(SEARCH_BRANCH);
        persistResults(crawlResults);
        try {
            // First commit changes to search branch and update remote
            String commitMessage = "Conducted search: " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            boolean newSearchResults = gitHandler.createCommitOnCurrentBranch(commitMessage, false);
            gitHandler.checkoutBranch(WORK_BRANCH);
            if (!newSearchResults) {
                return;
            }
            // Patch new results into work branch
            gitHandler.appendLatestSearchResultsOntoCurrentBranch(commitMessage + " - Patch", SEARCH_BRANCH);
            // Update both remote tracked branches
            updateRemoteSearchAndWorkBranch();
        } catch (GitAPIException e) {
            LOGGER.error("Updating remote repository failed", e);
        }
    }

    /**
     * Update the remote tracking branches of the work and search branches
     * The currently checked out branch is not changed if the method is executed successfully
     */
    private void updateRemoteSearchAndWorkBranch() throws IOException, GitAPIException {
        String currentBranch = gitHandler.getCurrentlyCheckedOutBranch();

        // update remote search branch
        gitHandler.checkoutBranch(SEARCH_BRANCH);
        gitHandler.pushCommitsToRemoteRepository();

        // update remote work branch
        gitHandler.checkoutBranch(WORK_BRANCH);
        gitHandler.pushCommitsToRemoteRepository();

        gitHandler.checkoutBranch(currentBranch);
    }

    /**
     * Updates the local work and search branches with changes from their tracking remote branches
     * The currently checked out branch is not changed if the method is executed successfully
     */
    private void updateWorkAndSearchBranch() throws IOException, GitAPIException {
        String currentBranch = gitHandler.getCurrentlyCheckedOutBranch();

        // update search branch
        gitHandler.checkoutBranch(SEARCH_BRANCH);
        gitHandler.pullOnCurrentBranch();

        // update work branch
        gitHandler.checkoutBranch(WORK_BRANCH);
        gitHandler.pullOnCurrentBranch();

        gitHandler.checkoutBranch(currentBranch);
    }

    /**
     * Create for each query a folder, and for each fetcher a bib file in the query folder to store its results.
     */
    private void setUpRepositoryStructureForQueriesAndFetchers() throws IOException {
        // Cannot use stream here since IOException has to be thrown
        StudyCatalogToFetcherConverter converter = new StudyCatalogToFetcherConverter(
                this.getActiveLibraryEntries(),
                preferencesService.getImportFormatPreferences(),
                preferencesService.getImporterPreferences());
        for (String query : this.getSearchQueryStrings()) {
            createQueryResultFolder(query);
            converter.getActiveFetchers()
                     .forEach(searchBasedFetcher -> createFetcherResultFile(query, searchBasedFetcher));
            createQueryResultFile(query);
        }
        createStudyResultFile();
    }

    /**
     * Creates a folder using the query and its corresponding query id.
     * This folder name is unique for each query, as long as the query id in the study definition is unique for each query.
     *
     * @param query The query the folder is created for
     */
    private void createQueryResultFolder(String query) throws IOException {
        Path queryResultFolder = getPathToQueryDirectory(query);
        createFolder(queryResultFolder);
    }

    private void createFolder(Path folder) throws IOException {
        if (Files.notExists(folder)) {
            Files.createDirectory(folder);
        }
    }

    private void createFetcherResultFile(String query, SearchBasedFetcher searchBasedFetcher) {
        String fetcherName = searchBasedFetcher.getName();
        Path fetcherResultFile = getPathToFetcherResultFile(query, fetcherName);
        createBibFile(fetcherResultFile);
    }

    private void createQueryResultFile(String query) {
        Path queryResultFile = getPathToFetcherResultFile(query, "result");
        createBibFile(queryResultFile);
    }

    private void createStudyResultFile() {
        createBibFile(getPathToStudyResultFile());
    }

    private void createBibFile(Path file) {
        if (Files.notExists(file)) {
            try {
                Files.createFile(file);
            } catch (IOException e) {
                throw new IllegalStateException("Error during creation of repository structure.", e);
            }
        }
    }

    /**
     * Returns a string that can be used as a folder name.
     * This removes all characters from the query that are illegal for directory names.
     * Structure: ID-trimmed query
     *
     * Examples:
     * Input: '(title: test-title AND abstract: Test)' as a query entry with id 12345678
     * Output: '12345678 - title= test-title AND abstract= Test'
     *
     * Input: 'abstract: Test*' as a query entry with id 87654321
     * Output: '87654321 - abstract= Test'
     *
     * Input: '"test driven"' as a query entry with id 12348765
     * Output: '12348765 - test driven'
     *
     * Note that this method might be similar to {@link org.jabref.logic.util.io.FileUtil#getValidFileName(String)} or {@link org.jabref.logic.util.io.FileNameCleaner#cleanFileName(String)}
     *
     * @param query that is trimmed and combined with its query id
     * @return a unique folder name for any query.
     */
    private String trimNameAndAddID(String query) {
        // Replace all field: with field= for folder name
        String trimmedNamed = MATCH_COLON.matcher(query).replaceAll("=");
        trimmedNamed = MATCH_ILLEGAL_CHARACTERS.matcher(trimmedNamed).replaceAll("");
        String id = computeIDForQuery(query);
        // Whole path has to be shorter than 260
        int remainingPathLength = 220 - studyDefinitionFile.toString().length() - id.length();
        if (query.length() > remainingPathLength) {
            trimmedNamed = query.substring(0, remainingPathLength);
        }
        return id + " - " + trimmedNamed;
    }

    /**
     * Helper to compute the query id for folder name creation.
     */
    private String computeIDForQuery(String query) {
        return String.valueOf(query.hashCode());
    }

    /**
     * Persists the crawling results in the local file based repository.
     *
     * @param crawlResults The results that shall be persisted.
     */
    private void persistResults(List<QueryResult> crawlResults) throws IOException, SaveException {
        DatabaseMerger merger = new DatabaseMerger(preferencesService.getBibEntryPreferences().getKeywordSeparator());
        BibDatabase newStudyResultEntries = new BibDatabase();

        for (QueryResult result : crawlResults) {
            BibDatabase queryResultEntries = new BibDatabase();
            for (FetchResult fetcherResult : result.getResultsPerFetcher()) {
                BibDatabase fetcherEntries = fetcherResult.getFetchResult();
                BibDatabaseContext existingFetcherResult = getFetcherResultEntries(result.getQuery(), fetcherResult.getFetcherName());

                // Merge new entries into fetcher result file
                merger.merge(existingFetcherResult.getDatabase(), fetcherEntries);

                // Create citation keys for all entries that do not have one
                generateCiteKeys(existingFetcherResult, fetcherEntries);

                // Aggregate each fetcher result into the query result
                merger.merge(queryResultEntries, fetcherEntries);

                writeResultToFile(getPathToFetcherResultFile(result.getQuery(), fetcherResult.getFetcherName()), existingFetcherResult.getDatabase());
            }
            BibDatabase existingQueryEntries = getQueryResultEntries(result.getQuery()).getDatabase();

            // Merge new entries into query result file
            merger.merge(existingQueryEntries, queryResultEntries);
            // Aggregate all new entries for every query into the study result
            merger.merge(newStudyResultEntries, queryResultEntries);

            writeResultToFile(getPathToQueryResultFile(result.getQuery()), existingQueryEntries);
        }
        BibDatabase existingStudyResultEntries = getStudyResultEntries().getDatabase();

        // Merge new entries into study result file
        merger.merge(existingStudyResultEntries, newStudyResultEntries);

        writeResultToFile(getPathToStudyResultFile(), existingStudyResultEntries);
    }

    private void generateCiteKeys(BibDatabaseContext existingEntries, BibDatabase targetEntries) {
        CitationKeyGenerator citationKeyGenerator = new CitationKeyGenerator(existingEntries,
                preferencesService.getCitationKeyPatternPreferences());
        targetEntries.getEntries().stream().filter(bibEntry -> !bibEntry.hasCitationKey()).forEach(citationKeyGenerator::generateAndSetKey);
    }

    private void writeResultToFile(Path pathToFile, BibDatabase entries) throws SaveException {
        try (AtomicFileWriter fileWriter = new AtomicFileWriter(pathToFile, StandardCharsets.UTF_8)) {
            SaveConfiguration saveConfiguration = new SaveConfiguration()
                    .withMetadataSaveOrder(true)
                    .withReformatOnSave(preferencesService.getLibraryPreferences().shouldAlwaysReformatOnSave());
            BibWriter bibWriter = new BibWriter(fileWriter, OS.NEWLINE);
            BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(
                    bibWriter,
                    saveConfiguration,
                    preferencesService.getFieldPreferences(),
                    preferencesService.getCitationKeyPatternPreferences(),
                    bibEntryTypesManager);
            databaseWriter.saveDatabase(new BibDatabaseContext(entries));
        } catch (UnsupportedCharsetException ex) {
            throw new SaveException(Localization.lang("Character encoding UTF-8 is not supported.", ex));
        } catch (IOException ex) {
            throw new SaveException("Problems saving", ex);
        }
    }

    private Path getPathToFetcherResultFile(String query, String fetcherName) {
        return repositoryPath.resolve(trimNameAndAddID(query)).resolve(FileNameCleaner.cleanFileName(fetcherName) + ".bib");
    }

    private Path getPathToQueryResultFile(String query) {
        return repositoryPath.resolve(trimNameAndAddID(query)).resolve("result.bib");
    }

    private Path getPathToStudyResultFile() {
        return repositoryPath.resolve(Crawler.FILENAME_STUDY_RESULT_BIB);
    }

    private Path getPathToQueryDirectory(String query) {
        return repositoryPath.resolve(trimNameAndAddID(query));
    }
}
