package org.jabref.gui.preferences.journals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JournalAbbreviationsViewModelTabTest {

    private final static TestAbbreviation ABBREVIATION_0 = new TestAbbreviation("Full0", "Abb0");
    private final static TestAbbreviation ABBREVIATION_0_OTHER_SHORT_UNIQUE = new TestAbbreviation("Full0", "Abb0", "A0");

    private final static TestAbbreviation ABBREVIATION_1 = new TestAbbreviation("Full1", "Abb1");
    private final static TestAbbreviation ABBREVIATION_1_SHOW = new TestAbbreviation("Full1", "Abb1", true);
    private final static TestAbbreviation ABBREVIATION_1_OTHER_SHORT_UNIQUE = new TestAbbreviation("Full1", "Abb1", "A1");

    private final static TestAbbreviation ABBREVIATION_2 = new TestAbbreviation("Full2", "Abb2");
    private final static TestAbbreviation ABBREVIATION_2_OTHER_SHORT_UNIQUE = new TestAbbreviation("Full2", "Abb2", "A2");

    private final static TestAbbreviation ABBREVIATION_3 = new TestAbbreviation("Full3", "Abb3");
    private final static TestAbbreviation ABBREVIATION_3_OTHER_SHORT_UNIQUE = new TestAbbreviation("Full3", "Abb3", "A3");

    private final static TestAbbreviation ABBREVIATION_4 = new TestAbbreviation("Full4", "Abb4");

    private final static TestAbbreviation ABBREVIATION_5 = new TestAbbreviation("Full5", "Abb5");

    private final static TestAbbreviation ABBREVIATION_6 = new TestAbbreviation("Full6", "Abb6");

    private JournalAbbreviationsTabViewModel viewModel;
    private Path emptyTestFile;
    private Path tempFolder;
    private final JournalAbbreviationRepository repository = JournalAbbreviationLoader.loadBuiltInRepository();
    private DialogService dialogService;

    static class TestAbbreviation extends Abbreviation {

        private final boolean showShortestUniqueAbbreviation;

        public TestAbbreviation(String name, String abbreviation) {
            this(name, abbreviation, false);
        }

        public TestAbbreviation(String name, String abbreviation, boolean showShortestUniqueAbbreviation) {
            super(name, abbreviation);
            this.showShortestUniqueAbbreviation = showShortestUniqueAbbreviation;
        }

        public TestAbbreviation(String name, String abbreviation, String shortestUniqueAbbreviation) {
            super(name, abbreviation, shortestUniqueAbbreviation);
            this.showShortestUniqueAbbreviation = true;
        }

        @Override
        public String toString() {
            if (showShortestUniqueAbbreviation) {
                return this.getName() + ";" + this.getAbbreviation() + ";" + this.getShortestUniqueAbbreviation();
            }
            return this.getName() + ";" + this.getAbbreviation();
        }
    }

    private static String csvListOfAbbreviations(List<TestAbbreviation> testAbbreviations) {
       return testAbbreviations.stream()
               .map(TestAbbreviation::toString)
               .collect(Collectors.joining("\n"));
    }

    private static String csvListOfAbbreviations(TestAbbreviation... testAbbreviations) {
        return csvListOfAbbreviations(Arrays.stream(testAbbreviations).toList());
    }

    private record CsvFileNameAndContent(String fileName, String content) {
        CsvFileNameAndContent(String fileName, TestAbbreviation... testAbbreviations) {
            this(fileName, csvListOfAbbreviations(testAbbreviations));
        }
    }

    private record TestData(
            List<CsvFileNameAndContent> csvFiles,
            TestAbbreviation abbreviationToCheck,
            List<String> finalContentsOfFile2,
            List<String> finalContentsOfFile3
    ) {
        /**
         * Note that we have a **different** ordering at the constructor, because Java generics have "type erasure"
         */
        public TestData(
                List<CsvFileNameAndContent> csvFiles,
                List<TestAbbreviation> finalContentsOfFile2,
                List<TestAbbreviation> finalContentsOfFile3,
                TestAbbreviation abbreviationToCheck
        ) {
            this(csvFiles,
                    abbreviationToCheck,
                    finalContentsOfFile2.stream().map(TestAbbreviation::toString).toList(),
                    finalContentsOfFile3.stream().map(TestAbbreviation::toString).toList());
        }
    }

    public static Stream<TestData> provideTestFiles() {
        // filenameing: testfileXY, where X is the number of test (count starts at 1), and Y is the number of the file in the test (count starts at 0)
        // testfile_3 has 5 entries after de-duplication
        return Stream.of(
                // No shortest unique abbreviations in files
                // Shortest unique abbreviations in entries (being the same then the abbreviation)
                new TestData(
                        List.of(
                                new CsvFileNameAndContent("testFile10.csv", ABBREVIATION_1),
                                new CsvFileNameAndContent("testFile11.csv", ABBREVIATION_0, ABBREVIATION_1, ABBREVIATION_2),
                                new CsvFileNameAndContent("testFile12.csv", ABBREVIATION_0, ABBREVIATION_1, ABBREVIATION_2, ABBREVIATION_3),
                                new CsvFileNameAndContent("testFile13.csv", ABBREVIATION_0, ABBREVIATION_1, ABBREVIATION_1, ABBREVIATION_2, ABBREVIATION_3, ABBREVIATION_4)),
                        List.of(ABBREVIATION_0, ABBREVIATION_1, ABBREVIATION_2, ABBREVIATION_5),
                        List.of(ABBREVIATION_0, ABBREVIATION_1, ABBREVIATION_2, ABBREVIATION_3, ABBREVIATION_6),
                        ABBREVIATION_1_SHOW),

                // Shortest unique abbreviations
                new TestData(
                        List.of(
                                new CsvFileNameAndContent("testFile20.csv", ABBREVIATION_1_OTHER_SHORT_UNIQUE),
                                new CsvFileNameAndContent("testFile21.csv", ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE),
                                new CsvFileNameAndContent("testFile22.csv", ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_3_OTHER_SHORT_UNIQUE),
                                // contains duplicate entry ABBREVIATION_1_OTHER_SHORT_UNIQUE, therefore 6 entries
                                new CsvFileNameAndContent("testFile23.csv", ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_3, ABBREVIATION_4)),
                        List.of(ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_5),
                        // without duplicates
                        List.of(ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_3, ABBREVIATION_6),
                        ABBREVIATION_1_OTHER_SHORT_UNIQUE),

                // Mixed abbreviations (some have shortest unique, some have not)
                new TestData(
                        List.of(
                                new CsvFileNameAndContent("testFile30.csv", ABBREVIATION_1),
                                new CsvFileNameAndContent("testFile31.csv", ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1, ABBREVIATION_2_OTHER_SHORT_UNIQUE),
                                new CsvFileNameAndContent("testFile32.csv", ABBREVIATION_0, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_3),
                                // contains ABBREVIATION_1 in two variants, therefore 5 in total
                                new CsvFileNameAndContent("testFile33.csv", ABBREVIATION_0, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_1, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_4)),
                        // Entries of testFile2 plus ABBREVIATION_5_SHOW minus ABBREVIATION_3
                        List.of(ABBREVIATION_0, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_5),
                        // Entries of testFile3 plus ABBREVIATION_6_SHOW minus ABBREVIATION_4
                        List.of(ABBREVIATION_0, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_1, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_6),
                        ABBREVIATION_1_OTHER_SHORT_UNIQUE)
        );
    }

    @BeforeEach
    void setUpViewModel(@TempDir Path tempFolder) throws Exception {
        JournalAbbreviationPreferences abbreviationPreferences = mock(JournalAbbreviationPreferences.class);

        dialogService = mock(DialogService.class);
        this.tempFolder = tempFolder;

        TaskExecutor taskExecutor = new CurrentThreadTaskExecutor();
        viewModel = new JournalAbbreviationsTabViewModel(abbreviationPreferences, dialogService, taskExecutor, repository);

        emptyTestFile = createTestFile(new CsvFileNameAndContent("emptyTestFile.csv", ""));
    }

    @Test
    void testInitialHasNoFilesAndNoAbbreviations() {
        assertEquals(0, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testInitialWithSavedFilesIncrementsFilesCounter(TestData testData) throws IOException {
        addFourTestFileToViewModelAndPreferences(testData);
        assertEquals(4, viewModel.journalFilesProperty().size());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testRemoveDuplicatesWhenReadingFiles(TestData testData) throws IOException {
        addFourTestFileToViewModelAndPreferences(testData);
        viewModel.selectLastJournalFile();

        assertEquals(4, viewModel.journalFilesProperty().size());
        assertEquals(5, viewModel.abbreviationsProperty().size());
    }

    @Test
    void addFileIncreasesCounterOfOpenFilesAndHasNoAbbreviations() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();

        assertEquals(1, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void addDuplicatedFileResultsInErrorDialog(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(0))));
        viewModel.addNewFile();
        viewModel.addNewFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testOpenDuplicatedFileResultsInAnException(TestData testData) throws IOException {
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(0))));
        viewModel.openFile();
        viewModel.openFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testSelectLastJournalFileSwitchesFilesAndTheirAbbreviations(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        assertEquals(0, viewModel.abbreviationsCountProperty().get());

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(0))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        assertEquals(1, viewModel.abbreviationsCountProperty().get());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testOpenValidFileContainsTheSpecificEntryAndEnoughAbbreviations(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(2))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();

        assertEquals(1, viewModel.journalFilesProperty().size());
        assertEquals(4, viewModel.abbreviationsProperty().size());

        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testData.abbreviationToCheck)));
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testRemoveLastListSetsCurrentFileAndCurrentAbbreviationToNull(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(0))));
        viewModel.addNewFile();
        viewModel.removeCurrentFile();

        assertEquals(0, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
        assertNull(viewModel.currentFileProperty().get());
        assertNull(viewModel.currentAbbreviationProperty().get());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testMixedFileUsage(TestData testData) throws IOException {
        // simulate open file button twice
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(1))));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(2))));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(1));

        // size of the list of journal files should be incremented by two
        assertEquals(2, viewModel.journalFilesProperty().size());

        // our third test file has 4 abbreviations
        assertEquals(4, viewModel.abbreviationsProperty().size());

        // check "arbitrary" abbreviation
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testData.abbreviationToCheck)));

        // simulate add new file button
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(2));

        // size of the list of journal files should be incremented by one
        assertEquals(3, viewModel.journalFilesProperty().size());

        // a new file has zero abbreviations
        assertEquals(0, viewModel.abbreviationsProperty().size());

        // simulate opening of testFile_3
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(3))));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(3));

        // size of the list of journal files should have been incremented by one
        assertEquals(4, viewModel.journalFilesProperty().size());

        // after de-duplication
        assertEquals(5, viewModel.abbreviationsProperty().size());

        // check "arbitrary" abbreviation
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testData.abbreviationToCheck)));
    }

    @Test
    void testBuiltInListsIncludeAllBuiltInAbbreviations() {
        viewModel.addBuiltInList();
        assertEquals(1, viewModel.journalFilesProperty().getSize());
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(0));
        ObservableList<Abbreviation> expected = FXCollections.observableArrayList(repository.getAllLoaded());
        ObservableList<Abbreviation> actualAbbreviations = FXCollections
                .observableArrayList(viewModel.abbreviationsProperty().stream()
                                              .map(AbbreviationViewModel::getAbbreviationObject)
                                              .collect(Collectors.toList()));

        assertEquals(expected, actualAbbreviations);
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testCurrentFilePropertyChangeActiveFile(TestData testData) throws IOException {
        for (CsvFileNameAndContent testFile : testData.csvFiles) {
            when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFile)));
            viewModel.addNewFile();
        }
        viewModel.selectLastJournalFile();

        AbbreviationsFileViewModel test1 = viewModel.journalFilesProperty().get(0);
        AbbreviationsFileViewModel test3 = viewModel.journalFilesProperty().get(1);
        AbbreviationsFileViewModel test4 = viewModel.journalFilesProperty().get(2);
        AbbreviationsFileViewModel test5 = viewModel.journalFilesProperty().get(3);

        // test if the last opened file is active, but duplicated entry has been removed
        assertEquals(5, viewModel.abbreviationsProperty().size());

        viewModel.currentFileProperty().set(test1);

        // test if the current abbreviations matches with the ones in testFile1Entries
        assertEquals(1, viewModel.abbreviationsProperty().size());

        viewModel.currentFileProperty().set(test3);
        assertEquals(3, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test1);
        assertEquals(1, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test4);
        assertEquals(4, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test5);
        assertEquals(5, viewModel.abbreviationsProperty().size());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testAddAbbreviationIncludesAbbreviationsInAbbreviationList(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(2))));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(3))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        viewModel.addAbbreviation(testAbbreviation);

        assertEquals(6, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testAddDuplicatedAbbreviationResultsInException(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        viewModel.addAbbreviation(new Abbreviation("YetAnotherEntry", "YAE"));
        viewModel.addAbbreviation(new Abbreviation("YetAnotherEntry", "YAE"));
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void testEditSameAbbreviationWithNoChangeDoesNotResultInException() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        viewModel.addAbbreviation(testAbbreviation);
        viewModel.editAbbreviation(testAbbreviation);

        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testEditAbbreviationIncludesNewAbbreviationInAbbreviationsList(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(2))));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(3))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        viewModel.addAbbreviation(testAbbreviation);

        assertEquals(6, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        // addAbbreviation(testAbbreviation);

        assertEquals(0, viewModel.abbreviationsProperty().size());
        assertFalse(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testEditAbbreviationToExistingOneResultsInException(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation(new Abbreviation("YetAnotherEntry", "YAE"));
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(1));
        viewModel.editAbbreviation(new Abbreviation("YetAnotherEntry", "YAE"));
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testEditAbbreviationToEmptyNameResultsInException(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation(new Abbreviation("", "YAE"));
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testEditAbbreviationToEmptyAbbreviationResultsInException(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation(new Abbreviation("YetAnotherEntry", ""));
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testSaveAbbreviationsToFilesCreatesNewFilesWithWrittenAbbreviations(TestData testData) throws Exception {
        Path testFile2 = createTestFile(testData.csvFiles.get(2));
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile2));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        viewModel.editAbbreviation(ABBREVIATION_5);
        assertEquals(4, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(ABBREVIATION_5)));

        Path testFile3 = createTestFile(testData.csvFiles.get(3));
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3));
        viewModel.addNewFile();
        assertEquals(5, viewModel.abbreviationsProperty().size());

        viewModel.selectLastJournalFile();
        assertTrue(viewModel.currentFileProperty().get().getAbsolutePath().get().getFileName().toString().endsWith("3.csv"));
        selectLastAbbreviation();
        viewModel.deleteAbbreviation();
        viewModel.addAbbreviation(ABBREVIATION_6);

        // deletion and addition of an entry leads to same size
        assertEquals(5, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(ABBREVIATION_6)));

        // overwrite all files
        viewModel.saveJournalAbbreviationFiles();

        List<String> actual = Files.readAllLines(testFile2, StandardCharsets.UTF_8);
        assertEquals(testData.finalContentsOfFile2, actual);

        actual = Files.readAllLines(testFile3, StandardCharsets.UTF_8);
        assertEquals(testData.finalContentsOfFile3, actual);
    }

    /**
     * Select the last abbreviation in the list of abbreviations
     */
    private void selectLastAbbreviation() {
        viewModel.currentAbbreviationProperty()
                 .set(viewModel.abbreviationsProperty().get(viewModel.abbreviationsCountProperty().get() - 1));
    }

    private void addFourTestFileToViewModelAndPreferences(TestData testData) throws IOException {
        for (CsvFileNameAndContent csvFile : testData.csvFiles) {
            when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(csvFile)));
            viewModel.addNewFile();
        }
        viewModel.storeSettings();
    }

    private Path createTestFile(CsvFileNameAndContent testFile) throws IOException {
        Path file = this.tempFolder.resolve(testFile.fileName);
        Files.writeString(file, testFile.content);
        return file;
    }
}
