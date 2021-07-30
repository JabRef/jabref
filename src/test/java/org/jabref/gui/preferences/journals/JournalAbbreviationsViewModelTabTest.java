package org.jabref.gui.preferences.journals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.jabref.logic.util.OS.NEWLINE;
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

    private JournalAbbreviationsTabViewModel viewModel;
    private Path emptyTestFile;
    private Path tempFolder;
    private PreferencesService preferencesService;
    private final JournalAbbreviationRepository repository = JournalAbbreviationLoader.loadBuiltInRepository();
    private DialogService dialogService;

    public static Stream<Arguments> provideTestFiles() {
        return Stream.of(
                // Mixed abbreviations
                Arguments.of(
                        List.of(List.of("testFile1Entries.csv", "Test Entry;TE" + NEWLINE + ""),
                                 List.of("testFile3Entries.csv", "Abbreviations;Abb;A" + NEWLINE + "Test Entry;TE" + NEWLINE + "MoreEntries;ME;M" + NEWLINE + ""),
                                 List.of("testFile4Entries.csv", "Abbreviations;Abb" + NEWLINE + "Test Entry;TE;T" + NEWLINE + "MoreEntries;ME;M" + NEWLINE + "Entry;E" + NEWLINE + ""),
                                 List.of("testFile5Entries.csv", "Abbreviations;Abb" + NEWLINE + "Test Entry;TE;T" + NEWLINE + "Test Entry;TE" + NEWLINE + "MoreEntries;ME;M" + NEWLINE + "EntryEntry;EE" + NEWLINE + "")),
                        List.of(
                                List.of("Abbreviations;Abb;Abb", "Test Entry;TE;T", "MoreEntries;ME;M", "JabRefTestEntry;JTE;JTE"),
                                List.of("EntryEntry;EE;EE", "Abbreviations;Abb;Abb", "Test Entry;TE;T", "SomeOtherEntry;SOE;SOE"))),

                // No shortest unique abbreviations
                Arguments.of(
                        List.of(List.of("testFile1Entries.csv", "Test Entry;TE" + NEWLINE + ""),
                                List.of("testFile3Entries.csv", "Abbreviations;Abb" + NEWLINE + "Test Entry;TE" + NEWLINE + "MoreEntries;ME" + NEWLINE + ""),
                                List.of("testFile4Entries.csv", "Abbreviations;Abb" + NEWLINE + "Test Entry;TE" + NEWLINE + "MoreEntries;ME" + NEWLINE + "Entry;E" + NEWLINE + ""),
                                List.of("testFile5Entries.csv", "Abbreviations;Abb" + NEWLINE + "Test Entry;TE" + NEWLINE + "Test Entry;TE" + NEWLINE + "MoreEntries;ME" + NEWLINE + "EntryEntry;EE" + NEWLINE + "")),
                        List.of(
                                List.of("Abbreviations;Abb;Abb", "Test Entry;TE;TE", "MoreEntries;ME;ME", "JabRefTestEntry;JTE;JTE"),
                               List.of("EntryEntry;EE;EE", "Abbreviations;Abb;Abb", "Test Entry;TE;TE", "SomeOtherEntry;SOE;SOE"))),

                // Shortest unique abbreviations
                Arguments.of(
                        List.of(List.of("testFile1Entries.csv", "Test Entry;TE;T" + NEWLINE + ""),
                                List.of("testFile3Entries.csv", "Abbreviations;Abb;A" + NEWLINE + "Test Entry;TE;T" + NEWLINE + "MoreEntries;ME;M" + NEWLINE + ""),
                                List.of("testFile4Entries.csv", "Abbreviations;Abb;A" + NEWLINE + "Test Entry;TE;T" + NEWLINE + "MoreEntries;ME;M" + NEWLINE + "Entry;En;E" + NEWLINE + ""),
                                List.of("testFile5Entries.csv", "Abbreviations;Abb;A" + NEWLINE + "Test Entry;TE;T" + NEWLINE + "Test Entry;TE;T" + NEWLINE + "MoreEntries;ME;M" + NEWLINE + "EntryEntry;EE" + NEWLINE + "")),
                        List.of(
                                List.of("Abbreviations;Abb;A", "Test Entry;TE;T", "MoreEntries;ME;M", "JabRefTestEntry;JTE;JTE"),
                                List.of("EntryEntry;EE;EE", "Abbreviations;Abb;A", "Test Entry;TE;T", "SomeOtherEntry;SOE;SOE")))
        );
    }

    @BeforeEach
    void setUpViewModel(@TempDir Path tempFolder) throws Exception {
        JournalAbbreviationPreferences abbreviationPreferences = mock(JournalAbbreviationPreferences.class);
        preferencesService = mock(PreferencesService.class);
        when(preferencesService.getJournalAbbreviationPreferences()).thenReturn(abbreviationPreferences);

        dialogService = mock(DialogService.class);
        this.tempFolder = tempFolder;

        TaskExecutor taskExecutor = new CurrentThreadTaskExecutor();
        viewModel = new JournalAbbreviationsTabViewModel(preferencesService, dialogService, taskExecutor, repository);

        emptyTestFile = createTestFile("emptyTestFile.csv", "");
    }

    @Test
    void testInitialHasNoFilesAndNoAbbreviations() {
        assertEquals(0, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testInitialWithSavedFilesIncrementsFilesCounter(List<List<String>> testFiles) throws IOException {
        addFourTestFileToViewModelAndPreferences(testFiles);
        assertEquals(4, viewModel.journalFilesProperty().size());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testRemoveDuplicatesWhenReadingFiles(List<List<String>> testFiles) throws IOException {
        addFourTestFileToViewModelAndPreferences(testFiles);
        viewModel.selectLastJournalFile();

        assertEquals(4, viewModel.journalFilesProperty().size());
        assertEquals(4, viewModel.abbreviationsProperty().size());
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
    void addDuplicatedFileResultsInErrorDialog(List<List<String>> testFiles) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(0))));
        viewModel.addNewFile();
        viewModel.addNewFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testOpenDuplicatedFileResultsInAnException(List<List<String>> testFiles) throws IOException {
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(0))));
        viewModel.openFile();
        viewModel.openFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testSelectLastJournalFileSwitchesFilesAndTheirAbbreviations(List<List<String>> testFiles) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        assertEquals(0, viewModel.abbreviationsCountProperty().get());

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(0))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        assertEquals(1, viewModel.abbreviationsCountProperty().get());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testOpenValidFileContainsTheSpecificEntryAndEnoughAbbreviations(List<List<String>> testFiles) throws IOException {
        Abbreviation testAbbreviation = new Abbreviation("Test Entry", "TE");
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();

        assertEquals(1, viewModel.journalFilesProperty().size());
        assertEquals(3, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testRemoveLastListSetsCurrentFileAndCurrentAbbreviationToNull(List<List<String>> testFiles) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(0))));
        viewModel.addNewFile();
        viewModel.removeCurrentFile();

        assertEquals(0, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
        assertNull(viewModel.currentFileProperty().get());
        assertNull(viewModel.currentAbbreviationProperty().get());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testMixedFileUsage(List<List<String>> testFiles) throws IOException {
        Abbreviation testAbbreviation = new Abbreviation("Entry", "E");
        Abbreviation testAbbreviation2 = new Abbreviation("EntryEntry", "EE");

        // simulate open file button twice
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(1))));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(2))));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(1));

        // size of the list of journal files should be incremented by two
        assertEquals(2, viewModel.journalFilesProperty().size());
        // our second test file has 4 abbreviations
        assertEquals(4, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        // simulate add new file button
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(2));

        // size of the list of journal files should be incremented by one
        assertEquals(3, viewModel.journalFilesProperty().size());
        // a new file has zero abbreviations
        assertEquals(0, viewModel.abbreviationsProperty().size());

        // simulate open file button
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(3))));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(3));

        // size of the list of journal files should be incremented by one
        assertEquals(4, viewModel.journalFilesProperty().size());

        assertEquals(4, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation2)));
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
    void testCurrentFilePropertyChangeActiveFile(List<List<String>> testFiles) throws IOException {
        for (List<String> testFile : testFiles) {
            when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFile)));
            viewModel.addNewFile();
        }
        viewModel.selectLastJournalFile();

        AbbreviationsFileViewModel test1 = viewModel.journalFilesProperty().get(0);
        AbbreviationsFileViewModel test3 = viewModel.journalFilesProperty().get(1);
        AbbreviationsFileViewModel test4 = viewModel.journalFilesProperty().get(2);
        AbbreviationsFileViewModel test5 = viewModel.journalFilesProperty().get(3);

        // test if the last opened file is active, but duplicated entry has been removed
        assertEquals(4, viewModel.abbreviationsProperty().size());

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
        assertEquals(4, viewModel.abbreviationsProperty().size());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testAddAbbreviationIncludesAbbreviationsInAbbreviationList(List<List<String>> testFiles) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(2))));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(3))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbreviation(testAbbreviation);

        assertEquals(5, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testAddDuplicatedAbbreviationResultsInException(List<List<String>> testFiles) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        viewModel.addAbbreviation("YetAnotherEntry", "YAE");
        viewModel.addAbbreviation("YetAnotherEntry", "YAE");
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void testEditSameAbbreviationWithNoChangeDoesNotResultInException() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbreviation(testAbbreviation);
        editAbbreviation(testAbbreviation);

        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testEditAbbreviationIncludesNewAbbreviationInAbbreviationsList(List<List<String>> testFiles) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(2))));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(3))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbreviation(testAbbreviation);

        assertEquals(5, viewModel.abbreviationsProperty().size());
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
    void testEditAbbreviationToExistingOneResultsInException(List<List<String>> testFiles) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("YetAnotherEntry", "YAE");
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(1));
        viewModel.editAbbreviation("YetAnotherEntry", "YAE");
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testEditAbbreviationToEmptyNameResultsInException(List<List<String>> testFiles) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("", "YAE");
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testEditAbbreviationToEmptyAbbreviationResultsInException(List<List<String>> testFiles) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("YetAnotherEntry", "");
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testSaveAbbreviationsToFilesCreatesNewFilesWithWrittenAbbreviations(List<List<String>> testFiles, List<List<String>> testEntries) throws Exception {
        Path testFile4Entries = createTestFile(testFiles.get(2));
        Path testFile5EntriesWithDuplicate = createTestFile(testFiles.get(3));

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        Abbreviation testAbbreviation = new Abbreviation("JabRefTestEntry", "JTE");
        editAbbreviation(testAbbreviation);

        assertEquals(4, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        viewModel.deleteAbbreviation();
        Abbreviation testAbbreviation1 = new Abbreviation("SomeOtherEntry", "SOE");
        addAbbreviation(testAbbreviation1);

        assertEquals(4, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation1)));

        viewModel.saveJournalAbbreviationFiles();

        List<String> actual = Files.readAllLines(testFile4Entries, StandardCharsets.UTF_8);
        assertEquals(testEntries.get(0), actual);

        actual = Files.readAllLines(testFile5EntriesWithDuplicate, StandardCharsets.UTF_8);
        assertEquals(testEntries.get(1), actual);
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void testSaveExternalFilesListToPreferences(List<List<String>> testFiles) throws IOException {
        addFourTestFileToViewModelAndPreferences(testFiles);
        verify(preferencesService).storeJournalAbbreviationPreferences(any());
    }

    private void addAbbreviation(Abbreviation testAbbreviation) {
        viewModel.addAbbreviation(testAbbreviation.getName(), testAbbreviation.getAbbreviation());
    }

    private void editAbbreviation(Abbreviation testAbbreviation) {
        viewModel.editAbbreviation(testAbbreviation.getName(), testAbbreviation.getAbbreviation());
    }

    /**
     * Select the last abbreviation in the list of abbreviations
     */
    private void selectLastAbbreviation() {
        viewModel.currentAbbreviationProperty()
                 .set(viewModel.abbreviationsProperty().get(viewModel.abbreviationsCountProperty().get() - 1));
    }

    private void addFourTestFileToViewModelAndPreferences(List<List<String>> testFiles) throws IOException {
        for (List<String> testFile : testFiles) {
            when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFile.get(0), testFile.get(1))));
            viewModel.addNewFile();
        }
        viewModel.storeSettings();
    }

    private Path createTestFile(String name, String content) throws IOException {
        Path file = this.tempFolder.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private Path createTestFile(List<String> testFile) throws IOException {
        return createTestFile(testFile.get(0), testFile.get(1));
    }
}
