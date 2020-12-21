package org.jabref.logic.crawler;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.crawler.git.GitHandler;
import org.jabref.logic.database.DatabaseMerger;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.SystematicLiteratureReviewStudyEntryType;
import org.jabref.model.study.FetchResult;
import org.jabref.model.study.QueryResult;
import org.jabref.model.study.Study;
import org.jabref.model.util.FileUpdateMonitor;

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
class StudyRepository {
    // Tests work with study.bib
    private static final String STUDY_DEFINITION_FILE_NAME = "study.bib";
    private static final Logger LOGGER = LoggerFactory.getLogger(StudyRepository.class);
    private static final Pattern MATCHCOLON = Pattern.compile(":");
    private static final Pattern MATCHILLEGALCHARACTERS = Pattern.compile("[^A-Za-z0-9_.\\s=-]");

    private final Path repositoryPath;
    private final Path studyDefinitionBib;
    private final GitHandler gitHandler;
    private final Study study;
    private final ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final SavePreferences savePreferences;
    private final BibEntryTypesManager bibEntryTypesManager;

    /**
     * Creates a study repository.
     *
     * @param pathToRepository Where the repository root is located.
     * @param gitHandler       The git handler that managages any interaction with the remote repository
     * @throws IllegalArgumentException If the repository root directory does not exist, or the root directory does not
     *                                  contain the study definition file.
     * @throws IOException              Thrown if the given repository does not exists, or the study definition file
     *                                  does not exist
     * @throws ParseException           Problem parsing the study definition file.
     */
    public StudyRepository(Path pathToRepository,
                           GitHandler gitHandler,
                           ImportFormatPreferences importFormatPreferences,
                           FileUpdateMonitor fileUpdateMonitor,
                           SavePreferences savePreferences,
                           BibEntryTypesManager bibEntryTypesManager) throws IOException, ParseException {
        this.repositoryPath = pathToRepository;
        this.gitHandler = gitHandler;
        try {
            gitHandler.updateLocalRepository();
        } catch (GitAPIException e) {
            LOGGER.error("Updating repository from remote failed");
        }
        this.importFormatPreferences = importFormatPreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.studyDefinitionBib = Path.of(repositoryPath.toString(), STUDY_DEFINITION_FILE_NAME);
        this.savePreferences = savePreferences;
        this.bibEntryTypesManager = bibEntryTypesManager;

        if (Files.notExists(repositoryPath)) {
            throw new IOException("The given repository does not exists.");
        } else if (Files.notExists(studyDefinitionBib)) {
            throw new IOException("The study definition file does not exist in the given repository.");
        }
        study = parseStudyFile();
        this.setUpRepositoryStructure();
    }

    /**
     * Returns entries stored in the repository for a certain query and fetcher
     */
    public BibDatabaseContext getFetcherResultEntries(String query, String fetcherName) throws IOException {
        return OpenDatabase.loadDatabase(getPathToFetcherResultFile(query, fetcherName), importFormatPreferences, fileUpdateMonitor).getDatabaseContext();
    }

    /**
     * Returns the merged entries stored in the repository for a certain query
     */
    public BibDatabaseContext getQueryResultEntries(String query) throws IOException {
        return OpenDatabase.loadDatabase(getPathToQueryResultFile(query), importFormatPreferences, fileUpdateMonitor).getDatabaseContext();
    }

    /**
     * Returns the merged entries stored in the repository for all queries
     */
    public BibDatabaseContext getStudyResultEntries() throws IOException {
        return OpenDatabase.loadDatabase(getPathToStudyResultFile(), importFormatPreferences, fileUpdateMonitor).getDatabaseContext();
    }

    /**
     * The study definition file contains all the definitions of a study. This method extracts the BibEntries from the study BiB file.
     *
     * @return Returns the BibEntries parsed from the study definition file.
     * @throws IOException    Problem opening the input stream.
     * @throws ParseException Problem parsing the study definition file.
     */
    private Study parseStudyFile() throws IOException, ParseException {
        BibtexParser parser = new BibtexParser(importFormatPreferences, fileUpdateMonitor);
        List<BibEntry> parsedEntries = new ArrayList<>();
        try (InputStream inputStream = Files.newInputStream(studyDefinitionBib)) {
            parsedEntries.addAll(parser.parseEntries(inputStream));
        }

        BibEntry studyEntry = parsedEntries.parallelStream()
                                           .filter(bibEntry -> bibEntry.getType().equals(SystematicLiteratureReviewStudyEntryType.STUDY_ENTRY)).findAny()
                                           .orElseThrow(() -> new ParseException("Study definition file does not contain a study entry"));
        List<BibEntry> queryEntries = parsedEntries.parallelStream()
                                                   .filter(bibEntry -> bibEntry.getType().equals(SystematicLiteratureReviewStudyEntryType.SEARCH_QUERY_ENTRY))
                                                   .collect(Collectors.toList());
        List<BibEntry> libraryEntries = parsedEntries.parallelStream()
                                                     .filter(bibEntry -> bibEntry.getType().equals(SystematicLiteratureReviewStudyEntryType.LIBRARY_ENTRY))
                                                     .collect(Collectors.toList());

        return new Study(studyEntry, queryEntries, libraryEntries);
    }

    public Study getStudy() {
        return study;
    }

    public void persist(List<QueryResult> crawlResults) throws IOException, GitAPIException {
        try {
            gitHandler.updateLocalRepository();
        } catch (GitAPIException e) {
            LOGGER.error("Updating repository from remote failed");
        }
        persistResults(crawlResults);
        study.setLastSearchDate(LocalDate.now());
        persistStudy();
        try {
            gitHandler.updateRemoteRepository("Conducted search " + LocalDate.now());
        } catch (GitAPIException e) {
            LOGGER.error("Updating remote repository failed");
        }
    }

    private void persistStudy() throws IOException {
        writeResultToFile(studyDefinitionBib, new BibDatabase(study.getAllEntries()));
    }

    /**
     * Create for each query a folder, and for each fetcher a bib file in the query folder to store its results.
     */
    private void setUpRepositoryStructure() throws IOException {
        // Cannot use stream here since IOException has to be thrown
        LibraryEntryToFetcherConverter converter = new LibraryEntryToFetcherConverter(study.getActiveLibraryEntries(), importFormatPreferences);
        for (String query : study.getSearchQueryStrings()) {
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
     * Input: '(title: test-title AND abstract: Test)' as a query entry with id 1
     * Output: '1 - title= test-title AND abstract= Test'
     *
     * Input: 'abstract: Test*' as a query entry with id 1
     * Output: '1 - abstract= Test'
     *
     * Input: '"test driven"' as a query entry with id 1
     * Output: '1 - test driven'
     *
     * @param query that is trimmed and combined with its query id
     * @return a unique folder name for any query.
     */
    private String trimNameAndAddID(String query) {
        // Replace all field: with field= for folder name
        String trimmedNamed = MATCHCOLON.matcher(query).replaceAll("=");
        trimmedNamed = MATCHILLEGALCHARACTERS.matcher(trimmedNamed).replaceAll("");
        if (query.length() > 240) {
            trimmedNamed = query.substring(0, 240);
        }
        String id = findQueryIDByQueryString(query);
        return id + " - " + trimmedNamed;
    }

    /**
     * Helper to find the query id for folder name creation.
     * Returns the id of the first SearchQuery BibEntry with a query field that matches the given query.
     *
     * @param query The query whose ID is searched
     * @return ID of the query defined in the study definition.
     */
    private String findQueryIDByQueryString(String query) {
        String queryField = "query";
        return study.getSearchQueryEntries()
                    .parallelStream()
                    .filter(bibEntry -> bibEntry.getField(new UnknownField(queryField)).orElse("").equals(query))
                    .map(BibEntry::getCitationKey)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .orElseThrow()
                    .replaceFirst(queryField, "");
    }

    /**
     * Persists the crawling results in the local file based repository.
     *
     * @param crawlResults The results that shall be persisted.
     */
    private void persistResults(List<QueryResult> crawlResults) throws IOException {
        DatabaseMerger merger = new DatabaseMerger(importFormatPreferences.getKeywordSeparator());
        BibDatabase newStudyResultEntries = new BibDatabase();

        for (QueryResult result : crawlResults) {
            BibDatabase queryResultEntries = new BibDatabase();
            for (FetchResult fetcherResult : result.getResultsPerFetcher()) {
                BibDatabase fetcherEntries = fetcherResult.getFetchResult();
                BibDatabaseContext existingFetcherResult = getFetcherResultEntries(result.getQuery(), fetcherResult.getFetcherName());

                // Create citation keys for all entries that do not have one
                generateCiteKeys(existingFetcherResult, fetcherEntries);

                // Merge new entries into fetcher result file
                merger.merge(existingFetcherResult.getDatabase(), fetcherEntries);
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
        CitationKeyGenerator citationKeyGenerator = new CitationKeyGenerator(existingEntries, savePreferences.getCitationKeyPatternPreferences());
        targetEntries.getEntries().stream().filter(bibEntry -> !bibEntry.hasCitationKey()).forEach(citationKeyGenerator::generateAndSetKey);
    }

    private void writeResultToFile(Path pathToFile, BibDatabase entries) throws IOException {
        try (Writer fileWriter = new FileWriter(pathToFile.toFile())) {
            BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(fileWriter, savePreferences, bibEntryTypesManager);
            databaseWriter.saveDatabase(new BibDatabaseContext(entries));
        }
    }

    private Path getPathToFetcherResultFile(String query, String fetcherName) {
        return Path.of(repositoryPath.toString(), trimNameAndAddID(query), fetcherName + ".bib");
    }

    private Path getPathToQueryResultFile(String query) {
        return Path.of(repositoryPath.toString(), trimNameAndAddID(query), "result.bib");
    }

    private Path getPathToStudyResultFile() {
        return Path.of(repositoryPath.toString(), "studyResult.bib");
    }

    private Path getPathToQueryDirectory(String query) {
        return Path.of(repositoryPath.toString(), trimNameAndAddID(query));
    }
}
