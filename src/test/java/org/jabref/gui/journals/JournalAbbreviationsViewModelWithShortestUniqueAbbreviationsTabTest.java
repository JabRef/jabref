package org.jabref.gui.journals;

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
import org.jabref.gui.preferences.journals.AbbreviationViewModel;
import org.jabref.gui.preferences.journals.AbbreviationsFileViewModel;
import org.jabref.gui.preferences.journals.JournalAbbreviationsTabViewModel;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.JabRefException;
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

class JournalAbbreviationsViewModelWithShortestUniqueAbbreviationsTabTest {

    private JournalAbbreviationsTabViewModel viewModel;
    private Path emptyTestFile;
    private Path testFile1Entries;
    private Path testFile3Entries;
    private Path testFile4Entries;
    private Path testFile5EntriesWithDuplicate;
    private JournalAbbreviationPreferences abbreviationPreferences;
    private DialogService dialogService;
    private final JournalAbbreviationRepository repository = JournalAbbreviationLoader.loadBuiltInRepository();

    @BeforeEach
    void setUpViewModel(@TempDir Path tempFolder) throws Exception {
        abbreviationPreferences = mock(JournalAbbreviationPreferences.class);
        PreferencesService preferences = mock(PreferencesService.class);
        when(preferences.getJournalAbbreviationPreferences()).thenReturn(abbreviationPreferences);

        dialogService = mock(DialogService.class);
        TaskExecutor taskExecutor = new CurrentThreadTaskExecutor();
        viewModel = new JournalAbbreviationsTabViewModel(preferences, dialogService, taskExecutor, repository);
        emptyTestFile = createTestFile(tempFolder, "emptyTestFile.csv", "");
        testFile1Entries = createTestFile(tempFolder, "testFile1Entries.csv", "Test Entry;TE;T" + NEWLINE + "");
        testFile3Entries = createTestFile(tempFolder, "testFile3Entries.csv", "Abbreviations;Abb;A" + NEWLINE + "Test Entry;TE;T" + NEWLINE + "MoreEntries;ME;M" + NEWLINE + "");
        testFile4Entries = createTestFile(tempFolder, "testFile4Entries.csv", "Abbreviations;Abb;A" + NEWLINE + "Test Entry;TE;T" + NEWLINE + "MoreEntries;ME;M" + NEWLINE + "Entry;En;E" + NEWLINE + "");
        testFile5EntriesWithDuplicate = createTestFile(tempFolder, "testFile5Entries.csv", "Abbreviations;Abb;A" + NEWLINE + "Test Entry;TE;T" + NEWLINE + "Test Entry;TE;T" + NEWLINE + "MoreEntries;ME;M" + NEWLINE + "EntryEntry;EE" + NEWLINE + "");
    }

    @Test
    void testInitialHasNoFilesAndNoAbbreviations() {
        assertEquals(0, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @Test
    void testInitialWithSavedFilesIncrementsFilesCounter() throws Exception {
        addFourTestFileToViewModelAndPreferences();
        viewModel.createFileObjects();

        assertEquals(4, viewModel.journalFilesProperty().size());
    }

    @Test
    void testRemoveDuplicatesWhenReadingFiles() throws Exception {
        addFourTestFileToViewModelAndPreferences();
        viewModel.createFileObjects();
        viewModel.selectLastJournalFile();

        // should result in 4 real abbreviations and one pseudo abbreviation
        assertEquals(5, viewModel.abbreviationsProperty().size());
    }

    @Test
    void addFileIncreasesCounterOfOpenFilesAndHasNoAbbreviations() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();

        assertEquals(1, viewModel.journalFilesProperty().size());
        assertEquals(1, viewModel.abbreviationsProperty().size());
    }

    @Test
    void addDuplicatedFileResultsInErrorDialog() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.addNewFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void testOpenDuplicatedFileResultsInAnException() throws Exception {
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.openFile();
        viewModel.openFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void testSelectLastJournalFileSwitchesFilesAndTheirAbbreviations() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        assertEquals(1, viewModel.abbreviationsCountProperty().get());

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        assertEquals(2, viewModel.abbreviationsCountProperty().get());
    }

    @Test
    void testOpenValidFileContainsTheSpecificEntryAndEnoughAbbreviations() throws Exception {
        Abbreviation testAbbreviation = new Abbreviation("Test Entry", "TE");
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();

        assertEquals(1, viewModel.journalFilesProperty().size());
        // our test file has 3 abbreviations and one pseudo abbreviation
        assertEquals(4, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    void testRemoveLastListSetsCurrentFileAndCurrentAbbreviationToNull() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.removeCurrentFile();

        assertEquals(0, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
        assertNull(viewModel.currentFileProperty().get());
        assertNull(viewModel.currentAbbreviationProperty().get());
    }

    @Test
    void testMixedFileUsage() throws Exception {
        Abbreviation testAbbreviation = new Abbreviation("Entry", "En", "E");
        Abbreviation testAbbreviation2 = new Abbreviation("EntryEntry", "EnEn", "EE");

        // simulate open file button twice
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(1));

        // size of the list of journal files should be incremented by two
        assertEquals(2, viewModel.journalFilesProperty().size());
        // our second test file has 4 abbreviations
        assertEquals(5, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        // simulate add new file button
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(2));

        // size of the list of journal files should be incremented by one
        assertEquals(3, viewModel.journalFilesProperty().size());
        // a new file has zero abbreviations
        assertEquals(1, viewModel.abbreviationsProperty().size());

        // simulate open file button
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(3));

        // size of the list of journal files should be incremented by one
        assertEquals(4, viewModel.journalFilesProperty().size());

        assertEquals(5, viewModel.abbreviationsProperty().size());
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
    void testcurrentFilePropertyChangeActiveFile() throws Exception {
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
        assertEquals(5, viewModel.abbreviationsProperty().size());

        viewModel.currentFileProperty().set(test1);

        // test if the current abbreviations matches with the ones in testFile1Entries
        assertEquals(2, viewModel.abbreviationsProperty().size());

        viewModel.currentFileProperty().set(test3);
        assertEquals(4, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test1);
        assertEquals(2, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test4);
        assertEquals(5, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test5);
        assertEquals(5, viewModel.abbreviationsProperty().size());
    }

    @Test
    void testAddAbbreviationIncludesAbbreviationsInAbbreviationList() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE", "Y");
        addAbbrevaition(testAbbreviation);

        assertEquals(6, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    void testAddDuplicatedAbbreviationResultsInException() throws JabRefException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        viewModel.addAbbreviation("YetAnotherEntry", "YAE", "Y");
        viewModel.addAbbreviation("YetAnotherEntry", "YAE", "Y");
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void testEditSameAbbreviationWithNoChangeDoesNotResultInException() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE", "Y");
        addAbbrevaition(testAbbreviation);
        editAbbreviation(testAbbreviation);

        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    void testEditAbbreviationIncludesNewAbbreviationInAbbreviationsList() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE", "Y");
        editAbbreviation(testAbbreviation);

        assertEquals(5, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        editAbbreviation(testAbbreviation);

        assertEquals(1, viewModel.abbreviationsProperty().size());
        assertFalse(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    void testEditAbbreviationToExistingOneResultsInException() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(4, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("YetAnotherEntry", "YAE", "Y");
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(2));
        viewModel.editAbbreviation("YetAnotherEntry", "YAE", "Y");
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void testEditAbbreviationToEmptyNameResultsInException() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(4, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("", "YAE", "Y");
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @Test
    void testEditAbbreviationToEmptyAbbreviationResultsInException() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(4, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("YetAnotherEntry", "", "Y");
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @Test
    void testDeleteAbbreviationSelectsPreviousOne() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE", "Y");
        addAbbrevaition(testAbbreviation);

        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
        assertEquals(new AbbreviationViewModel(testAbbreviation), viewModel.currentAbbreviationProperty().get());

        viewModel.deleteAbbreviation();

        assertEquals(5, viewModel.abbreviationsProperty().size());
        // check if the previous (the last) element is the current abbreviation
        assertEquals(viewModel.currentAbbreviationProperty().get(), viewModel.abbreviationsProperty().get(4));
    }

    @Test
    void testDeleteAbbreviationSelectsNextOne() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE", "Y");
        addAbbrevaition(testAbbreviation);
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(1));
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

        assertEquals(5, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        viewModel.deleteAbbreviation();
        Abbreviation testAbbreviation1 = new Abbreviation("SomeOtherEntry", "SOE");
        addAbbrevaition(testAbbreviation1);

        assertEquals(5, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation1)));

        viewModel.saveJournalAbbreviationFiles();
        List<String> expected = Arrays.asList(
                "Abbreviations;Abb;A",
                "Test Entry;TE;T",
                "MoreEntries;ME;M",
                "JabRefTestEntry;JTE;JTE");
        List<String> actual = Files.readAllLines(testFile4Entries, StandardCharsets.UTF_8);

        assertEquals(expected, actual);

        expected = Arrays.asList(
                "EntryEntry;EE;EE",
                "Abbreviations;Abb;A",
                "Test Entry;TE;T",
                "SomeOtherEntry;SOE;SOE");
        actual = Files.readAllLines(testFile5EntriesWithDuplicate, StandardCharsets.UTF_8);

        assertEquals(expected, actual);
    }

    @Test
    void testSaveExternalFilesListToPreferences() throws Exception {
        addFourTestFileToViewModelAndPreferences();
        List<String> expected = Stream.of(testFile1Entries, testFile3Entries, testFile4Entries, testFile5EntriesWithDuplicate)
                                      .map(Path::toString).collect(Collectors.toList());
        verify(abbreviationPreferences).setExternalJournalLists(expected);
    }

    private Path createTestFile(Path folder, String name, String content) throws Exception {
        Path file = folder.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private void addAbbrevaition(Abbreviation testAbbreviation) throws Exception {
        viewModel.addAbbreviation(testAbbreviation.getName(), testAbbreviation.getAbbreviation());
    }

    private void editAbbreviation(Abbreviation testAbbreviation) throws Exception {
        viewModel.editAbbreviation(testAbbreviation.getName(), testAbbreviation.getAbbreviation());
    }

    private void addFourTestFileToViewModelAndPreferences() throws Exception {
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
