package org.jabref.logic.crawler;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.database.DatabaseMerger;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.git.SlrGitHandler;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.study.FetchResult;
import org.jabref.model.study.QueryResult;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.jabref.logic.citationkeypattern.CitationKeyGenerator.DEFAULT_UNWANTED_CHARACTERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudyRepositoryTest {
    private static final String NON_EXISTING_DIRECTORY = "nonExistingTestRepositoryDirectory";
    CitationKeyPatternPreferences citationKeyPatternPreferences;
    GeneralPreferences generalPreferences;
    ImportFormatPreferences importFormatPreferences;
    SavePreferences savePreferences;
    TimestampPreferences timestampPreferences;
    BibEntryTypesManager entryTypesManager;
    @TempDir
    Path tempRepositoryDirectory;
    StudyRepository studyRepository;
    SlrGitHandler gitHandler = mock(SlrGitHandler.class, Answers.RETURNS_DEFAULTS);
    String hashCodeQuantum = String.valueOf("Quantum".hashCode());
    String hashCodeCloudComputing = String.valueOf("Cloud Computing".hashCode());
    String hashCodeSoftwareEngineering = String.valueOf("\"Software Engineering\"".hashCode());

    /**
     * Set up mocks
     */
    @BeforeEach
    public void setUpMocks() throws Exception {
        generalPreferences = mock(GeneralPreferences.class, Answers.RETURNS_DEEP_STUBS);
        savePreferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        timestampPreferences = mock(TimestampPreferences.class);
        citationKeyPatternPreferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                GlobalCitationKeyPattern.fromPattern("[auth][year]"),
                ',');
        when(generalPreferences.getDefaultEncoding()).thenReturn(Charset.defaultCharset());
        when(savePreferences.getSaveOrder()).thenReturn(new SaveOrderConfig());
        when(savePreferences.takeMetadataSaveOrderInAccount()).thenReturn(true);
        when(savePreferences.getCitationKeyPatternPreferences()).thenReturn(citationKeyPatternPreferences);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(new FieldContentFormatterPreferences());
        when(importFormatPreferences.isKeywordSyncEnabled()).thenReturn(false);
        when(timestampPreferences.getTimestampField()).then(invocation -> StandardField.TIMESTAMP);
        entryTypesManager = new BibEntryTypesManager();
        getTestStudyRepository();
    }

    @Test
    void providePathToNonExistentRepositoryThrowsException() {
        Path nonExistingRepositoryDirectory = tempRepositoryDirectory.resolve(NON_EXISTING_DIRECTORY);

        assertThrows(IOException.class, () -> new StudyRepository(nonExistingRepositoryDirectory, gitHandler, generalPreferences, importFormatPreferences, new DummyFileUpdateMonitor(), savePreferences, entryTypesManager));
    }

    /**
     * Tests whether the file structure of the repository is created correctly from the study definitions file.
     */
    @Test
    void repositoryStructureCorrectlyCreated() throws Exception {

        // When repository is instantiated the directory structure is created
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeQuantum + " - Quantum")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeCloudComputing + " - Cloud Computing")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeSoftwareEngineering + " - Software Engineering")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeQuantum + " - Quantum", "ArXiv.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeCloudComputing + " - Cloud Computing", "ArXiv.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeSoftwareEngineering + " - Software Engineering", "ArXiv.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeQuantum + " - Quantum", "Springer.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeCloudComputing + " - Cloud Computing", "Springer.bib")));
        assertTrue(Files.exists(Path.of(tempRepositoryDirectory.toString(), hashCodeSoftwareEngineering + " - Software Engineering", "Springer.bib")));
        assertTrue(Files.notExists(Path.of(tempRepositoryDirectory.toString(), hashCodeQuantum + " - Quantum", "IEEEXplore.bib")));
        assertTrue(Files.notExists(Path.of(tempRepositoryDirectory.toString(), hashCodeCloudComputing + " - Cloud Computing", "IEEEXplore.bib")));
        assertTrue(Files.notExists(Path.of(tempRepositoryDirectory.toString(), hashCodeSoftwareEngineering + " - Software Engineering", "IEEEXplore.bib")));
    }

    /**
     * This tests whether the repository returns the stored bib entries correctly.
     */
    @Test
    void bibEntriesCorrectlyStored() throws Exception {
        setUpTestResultFile();
        List<BibEntry> result = studyRepository.getFetcherResultEntries("Quantum", "ArXiv").getEntries();
        assertEquals(getArXivQuantumMockResults(), result);
    }

    @Test
    void fetcherResultsPersistedCorrectly() throws Exception {
        List<QueryResult> mockResults = getMockResults();

        studyRepository.persist(mockResults);

        assertEquals(getArXivQuantumMockResults(), getTestStudyRepository().getFetcherResultEntries("Quantum", "ArXiv").getEntries());
        assertEquals(getSpringerQuantumMockResults(), getTestStudyRepository().getFetcherResultEntries("Quantum", "Springer").getEntries());
        assertEquals(getSpringerCloudComputingMockResults(), getTestStudyRepository().getFetcherResultEntries("Cloud Computing", "Springer").getEntries());
    }

    @Test
    void mergedResultsPersistedCorrectly() throws Exception {
        List<QueryResult> mockResults = getMockResults();
        List<BibEntry> expected = new ArrayList<>();
        expected.addAll(getArXivQuantumMockResults());
        expected.add(getSpringerQuantumMockResults().get(1));
        expected.add(getSpringerQuantumMockResults().get(2));

        studyRepository.persist(mockResults);

        // All Springer results are duplicates for "Quantum"
        assertEquals(expected, getTestStudyRepository().getQueryResultEntries("Quantum").getEntries());
        assertEquals(getSpringerCloudComputingMockResults(), getTestStudyRepository().getQueryResultEntries("Cloud Computing").getEntries());
    }

    @Test
    void setsLastSearchDatePersistedCorrectly() throws Exception {
        List<QueryResult> mockResults = getMockResults();

        studyRepository.persist(mockResults);

        assertEquals(LocalDate.now(), getTestStudyRepository().getStudy().getLastSearchDate());
    }

    @Test
    void studyResultsPersistedCorrectly() throws Exception {
        List<QueryResult> mockResults = getMockResults();

        studyRepository.persist(mockResults);

        assertEquals(new HashSet<>(getNonDuplicateBibEntryResult().getEntries()), new HashSet<>(getTestStudyRepository().getStudyResultEntries().getEntries()));
    }

    private StudyRepository getTestStudyRepository() throws Exception {
        setUpTestStudyDefinitionFile();
        studyRepository = new StudyRepository(tempRepositoryDirectory, gitHandler, generalPreferences, importFormatPreferences, new DummyFileUpdateMonitor(), savePreferences, entryTypesManager);
        return studyRepository;
    }

    /**
     * Copies the study definition file into the test repository
     */
    private void setUpTestStudyDefinitionFile() throws Exception {
        Path destination = tempRepositoryDirectory.resolve("study.yml");
        URL studyDefinition = this.getClass().getResource("study.yml");
        FileUtil.copyFile(Path.of(studyDefinition.toURI()), destination, false);
    }

    /**
     * This overwrites the existing result file in the repository with a result file containing multiple BibEntries.
     * The repository has to exist before this method is called.
     */
    private void setUpTestResultFile() throws Exception {
        Path queryDirectory = Path.of(tempRepositoryDirectory.toString(), hashCodeQuantum + " - Quantum");
        Path resultFileLocation = Path.of(queryDirectory.toString(), "ArXiv" + ".bib");
        URL resultFile = this.getClass().getResource("ArXivQuantumMock.bib");
        FileUtil.copyFile(Path.of(resultFile.toURI()), resultFileLocation, true);
        resultFileLocation = Path.of(queryDirectory.toString(), "Springer" + ".bib");
        resultFile = this.getClass().getResource("SpringerQuantumMock.bib");
        FileUtil.copyFile(Path.of(resultFile.toURI()), resultFileLocation, true);
    }

    private BibDatabase getNonDuplicateBibEntryResult() {
        BibDatabase mockResults = new BibDatabase(getSpringerCloudComputingMockResults());
        DatabaseMerger merger = new DatabaseMerger(importFormatPreferences.getKeywordSeparator());
        merger.merge(mockResults, new BibDatabase(getSpringerQuantumMockResults()));
        merger.merge(mockResults, new BibDatabase(getArXivQuantumMockResults()));
        return mockResults;
    }

    private List<QueryResult> getMockResults() {
        QueryResult resultQuantum =
                new QueryResult("Quantum", List.of(
                        new FetchResult("ArXiv", new BibDatabase(stripCitationKeys(getArXivQuantumMockResults()))),
                        new FetchResult("Springer", new BibDatabase(stripCitationKeys(getSpringerQuantumMockResults())))));
        QueryResult resultCloudComputing = new QueryResult("Cloud Computing", List.of(new FetchResult("Springer", new BibDatabase(getSpringerCloudComputingMockResults()))));
        return List.of(resultQuantum, resultCloudComputing);
    }

    /**
     * Strips the citation key from fetched entries as these normally do not have a citation key
     */
    private List<BibEntry> stripCitationKeys(List<BibEntry> entries) {
        entries.forEach(bibEntry -> bibEntry.setCitationKey(""));
        return entries;
    }

    private List<BibEntry> getArXivQuantumMockResults() {
        BibEntry entry1 = new BibEntry()
                .withCitationKey("Blaha")
                .withField(StandardField.AUTHOR, "Stephen Blaha")
                .withField(StandardField.TITLE, "Quantum Computers and Quantum Computer Languages: Quantum Assembly Language and Quantum C Language");
        entry1.setType(StandardEntryType.Article);
        BibEntry entry2 = new BibEntry()
                .withCitationKey("Kaye")
                .withField(StandardField.AUTHOR, "Phillip Kaye and Michele Mosca")
                .withField(StandardField.TITLE, "Quantum Networks for Generating Arbitrary Quantum States");
        entry2.setType(StandardEntryType.Article);
        BibEntry entry3 = new BibEntry()
                .withCitationKey("Watrous")
                .withField(StandardField.AUTHOR, "John Watrous")
                .withField(StandardField.TITLE, "Quantum Computational Complexity");
        entry3.setType(StandardEntryType.Article);

        return List.of(entry1, entry2, entry3);
    }

    private List<BibEntry> getSpringerQuantumMockResults() {
        // This is a duplicate of entry 1 of ArXiv
        BibEntry entry1 = new BibEntry()
                .withCitationKey("Blaha")
                .withField(StandardField.AUTHOR, "Stephen Blaha")
                .withField(StandardField.TITLE, "Quantum Computers and Quantum Computer Languages: Quantum Assembly Language and Quantum C Language");
        entry1.setType(StandardEntryType.Article);
        BibEntry entry2 = new BibEntry()
                .withCitationKey("Kroeger")
                .withField(StandardField.AUTHOR, "H. Kröger")
                .withField(StandardField.TITLE, "Nonlinear Dynamics In Quantum Physics -- Quantum Chaos and Quantum Instantons");
        entry2.setType(StandardEntryType.Article);
        BibEntry entry3 = new BibEntry()
                .withField(StandardField.AUTHOR, "Zieliński, Cezary")
                .withField(StandardField.TITLE, "Automatic Control, Robotics, and Information Processing");
        entry3.setType(StandardEntryType.Article);

        CitationKeyGenerator citationKeyGenerator = new CitationKeyGenerator(new BibDatabaseContext(), citationKeyPatternPreferences);
        citationKeyGenerator.generateAndSetKey(entry3);

        return List.of(entry1, entry2, entry3);
    }

    private List<BibEntry> getSpringerCloudComputingMockResults() {
        BibEntry entry1 = new BibEntry()
                .withCitationKey("Gritzalis")
                .withField(StandardField.AUTHOR, "Gritzalis, Dimitris and Stergiopoulos, George and Vasilellis, Efstratios and Anagnostopoulou, Argiro")
                .withField(StandardField.TITLE, "Readiness Exercises: Are Risk Assessment Methodologies Ready for the Cloud?");
        entry1.setType(StandardEntryType.Article);
        BibEntry entry2 = new BibEntry()
                .withCitationKey("Rangras")
                .withField(StandardField.AUTHOR, "Rangras, Jimit and Bhavsar, Sejal")
                .withField(StandardField.TITLE, "Design of Framework for Disaster Recovery in Cloud Computing");
        entry2.setType(StandardEntryType.Article);
        return List.of(entry1, entry2);
    }
}
