package org.jabref.gui.journals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.journals.AbbreviationViewModel;
import org.jabref.gui.preferences.journals.AbbreviationsFileViewModel;
import org.jabref.gui.preferences.journals.JournalAbbreviationsTabViewModel;
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

class JournalAbbreviationsViewModelNoShortestUniqueAbbreviationsTabTest {

    private JournalAbbreviationsTabViewModel viewModel;
    private Path emptyTestFile;
    private Path testFile1Entries;
    private Path testFile3Entries;
    private Path testFile4Entries;
    private Path testFile5EntriesWithDuplicate;
    private PreferencesService preferencesService;
    private DialogService dialogService;
    private final JournalAbbreviationRepository repository = JournalAbbreviationLoader.loadBuiltInRepository();

    @BeforeEach
    void setUpViewModel(@TempDir Path tempFolder) throws Exception {
        JournalAbbreviationPreferences abbreviationPreferences = mock(JournalAbbreviationPreferences.class);
        preferencesService = mock(PreferencesService.class);
        when(preferencesService.getJournalAbbreviationPreferences()).thenReturn(abbreviationPreferences);

        dialogService = mock(DialogService.class);
        TaskExecutor taskExecutor = new CurrentThreadTaskExecutor();
        viewModel = new JournalAbbreviationsTabViewModel(preferencesService, dialogService, taskExecutor, repository);
        emptyTestFile = createTestFile(tempFolder, "emptyTestFile.csv", "");
        testFile1Entries = createTestFile(tempFolder, "testFile1Entries.csv", "Test Entry;TE" + NEWLINE + "");
        testFile3Entries = createTestFile(tempFolder, "testFile3Entries.csv", "Abbreviations;Abb" + NEWLINE + "Test Entry;TE" + NEWLINE + "MoreEntries;ME" + NEWLINE + "");
        testFile4Entries = createTestFile(tempFolder, "testFile4Entries.csv", "Abbreviations;Abb" + NEWLINE + "Test Entry;TE" + NEWLINE + "MoreEntries;ME" + NEWLINE + "Entry;E" + NEWLINE + "");
        testFile5EntriesWithDuplicate = createTestFile(tempFolder, "testFile5Entries.csv", "Abbreviations;Abb" + NEWLINE + "Test Entry;TE" + NEWLINE + "Test Entry;TE" + NEWLINE + "MoreEntries;ME" + NEWLINE + "EntryEntry;EE" + NEWLINE + "");
    }

    @Test
    void testInitialHasNoFilesAndNoAbbreviations() {
        assertEquals(0, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @Test
    void testInitialWithSavedFilesIncrementsFilesCounter() {
        addFourTestFileToViewModelAndPreferences();
        viewModel.createFileObjects();

        assertEquals(4, viewModel.journalFilesProperty().size());
    }

    @Test
    void testRemoveDuplicatesWhenReadingFiles() {
        addFourTestFileToViewModelAndPreferences();
        viewModel.createFileObjects();
        viewModel.selectLastJournalFile();

        assertEquals(4, viewModel.abbreviationsProperty().size());
    }

    @Test
    void addFileIncreasesCounterOfOpenFilesAndHasNoAbbreviations() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();

        assertEquals(1, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @Test
    void addDuplicatedFileResultsInErrorDialog() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.addNewFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void testOpenDuplicatedFileResultsInAnException() {
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.openFile();
        viewModel.openFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void testSelectLastJournalFileSwitchesFilesAndTheirAbbreviations() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        assertEquals(0, viewModel.abbreviationsCountProperty().get());

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        assertEquals(1, viewModel.abbreviationsCountProperty().get());
    }

    @Test
    void testOpenValidFileContainsTheSpecificEntryAndEnoughAbbreviations() {
        Abbreviation testAbbreviation = new Abbreviation("Test Entry", "TE");
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();

        assertEquals(1, viewModel.journalFilesProperty().size());
        assertEquals(3, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    void testRemoveLastListSetsCurrentFileAndCurrentAbbreviationToNull() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.removeCurrentFile();

        assertEquals(0, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
        assertNull(viewModel.currentFileProperty().get());
        assertNull(viewModel.currentAbbreviationProperty().get());
    }

    @Test
    void testMixedFileUsage() {
        Abbreviation testAbbreviation = new Abbreviation("Entry", "E");
        Abbreviation testAbbreviation2 = new Abbreviation("EntryEntry", "EE");

        // simulate open file button twice
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
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
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
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
        ObservableList<Abbreviation> expected = FXCollections
                .observableArrayList(repository.getAllLoaded());
        ObservableList<Abbreviation> actualAbbreviations = FXCollections
                .observableArrayList(viewModel.abbreviationsProperty().stream()
                                              .map(AbbreviationViewModel::getAbbreviationObject).collect(Collectors.toList()));

        assertEquals(expected, actualAbbreviations);
    }

    @Test
    void testcurrentFilePropertyChangeActiveFile() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
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

    @Test
    void testAddAbbreviationIncludesAbbreviationsInAbbreviationList() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbreviation(testAbbreviation);

        assertEquals(5, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    void testAddDuplicatedAbbreviationResultsInException() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
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

    @Test
    void testEditAbbreviationIncludesNewAbbreviationInAbbreviationsList() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
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
        // editAbbreviation(testAbbreviation);

        assertEquals(0, viewModel.abbreviationsProperty().size());
        assertFalse(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    void testEditAbbreviationToExistingOneResultsInException() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("YetAnotherEntry", "YAE");
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(1));
        viewModel.editAbbreviation("YetAnotherEntry", "YAE");
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void testEditAbbreviationToEmptyNameResultsInException() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("", "YAE");
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @Test
    void testEditAbbreviationToEmptyAbbreviationResultsInException() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("YetAnotherEntry", "");
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @Test
    void testDeleteAbbreviationSelectsPreviousOne() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbreviation(testAbbreviation);

        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
        assertEquals(new AbbreviationViewModel(testAbbreviation), viewModel.currentAbbreviationProperty().get());

        viewModel.deleteAbbreviation();

        assertEquals(4, viewModel.abbreviationsProperty().size());
        // check if the previous (the last) element is the current abbreviation
        assertEquals(viewModel.currentAbbreviationProperty().get(), viewModel.abbreviationsProperty().get(3));
    }

    @Test
    void testDeleteAbbreviationSelectsNextOne() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbreviation(testAbbreviation);
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(0));
        viewModel.deleteAbbreviation();

        assertEquals(new AbbreviationViewModel(testAbbreviation), viewModel.currentAbbreviationProperty().get());
    }

    @Test
    void testSaveAbbreviationsToFilesCreatesNewFilesWithWrittenAbbreviations() throws Exception {
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
        List<String> expected = Arrays.asList(
                "Abbreviations;Abb;Abb",
                "Test Entry;TE;TE",
                "MoreEntries;ME;ME",
                "JabRefTestEntry;JTE;JTE");
        List<String> actual = Files.readAllLines(testFile4Entries, StandardCharsets.UTF_8);

        assertEquals(expected, actual);

        expected = Arrays.asList(
                "EntryEntry;EE;EE",
                "Abbreviations;Abb;Abb",
                "Test Entry;TE;TE",
                "SomeOtherEntry;SOE;SOE");
        actual = Files.readAllLines(testFile5EntriesWithDuplicate, StandardCharsets.UTF_8);

        assertEquals(expected, actual);
    }

    @Test
    void testSaveExternalFilesListToPreferences() {
        addFourTestFileToViewModelAndPreferences();
        verify(preferencesService).storeJournalAbbreviationPreferences(any());
    }

    private Path createTestFile(Path folder, String name, String content) throws Exception {
        Path file = folder.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private void addAbbreviation(Abbreviation testAbbreviation) {
        viewModel.addAbbreviation(testAbbreviation.getName(), testAbbreviation.getAbbreviation());
    }

    private void editAbbreviation(Abbreviation testAbbreviation) {
        viewModel.editAbbreviation(testAbbreviation.getName(), testAbbreviation.getAbbreviation());
    }

    private void addFourTestFileToViewModelAndPreferences() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.storeSettings();
    }

    /**
     * Select the last abbreviation in the list of abbreviations
     */
    private void selectLastAbbreviation() {
        viewModel.currentAbbreviationProperty()
                 .set(viewModel.abbreviationsProperty().get(viewModel.abbreviationsCountProperty().get() - 1));
    }
}
