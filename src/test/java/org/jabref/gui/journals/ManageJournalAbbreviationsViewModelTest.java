package org.jabref.gui.journals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.CatchExceptionsFromThread;
import org.jabref.JabRefException;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.preferences.PreferencesService;

import org.assertj.core.util.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.jabref.logic.util.OS.NEWLINE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ManageJournalAbbreviationsViewModelTest {

    @ClassRule
    public static CatchExceptionsFromThread catchExceptions = new CatchExceptionsFromThread();
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private ManageJournalAbbreviationsViewModel viewModel;
    private Path emptyTestFile;
    private Path testFile1Entries;
    private Path testFile3Entries;
    private Path testFile4Entries;
    private Path testFile5EntriesWithDuplicate;
    private JournalAbbreviationPreferences abbreviationPreferences;
    private DialogService dialogService;

    @Before
    public void setUpViewModel() throws Exception {
        abbreviationPreferences = mock(JournalAbbreviationPreferences.class);
        PreferencesService preferences = mock(PreferencesService.class);
        when(preferences.getJournalAbbreviationPreferences()).thenReturn(abbreviationPreferences);

        dialogService = mock(DialogService.class);
        TaskExecutor taskExecutor = new CurrentThreadTaskExecutor();
        JournalAbbreviationLoader journalAbbreviationLoader = mock(JournalAbbreviationLoader.class);
        viewModel = new ManageJournalAbbreviationsViewModel(preferences, dialogService, taskExecutor, journalAbbreviationLoader);
        emptyTestFile = createTemporaryTestFile("emptyTestFile.txt", "");
        testFile1Entries = createTemporaryTestFile("testFile1Entries.txt", "Test Entry = TE" + NEWLINE + "");
        testFile3Entries = createTemporaryTestFile("testFile3Entries.txt",
                "Abbreviations = Abb" + NEWLINE + "Test Entry = TE" + NEWLINE + "MoreEntries = ME" + NEWLINE + "");
        testFile4Entries = createTemporaryTestFile("testFile4Entries.txt", "Abbreviations = Abb" + NEWLINE
                + "Test Entry = TE" + NEWLINE + "MoreEntries = ME" + NEWLINE + "Entry = E" + NEWLINE + "");
        testFile5EntriesWithDuplicate = createTemporaryTestFile("testFile5Entries.txt",
                "Abbreviations = Abb" + NEWLINE + "Test Entry = TE" + NEWLINE + "Test Entry = TE" + NEWLINE
                        + "MoreEntries = ME" + NEWLINE + "EntryEntry = EE" + NEWLINE + "");
    }

    @Test
    public void testInitialHasNoFilesAndNoAbbreviations() {
        Assert.assertEquals(0, viewModel.journalFilesProperty().size());
        Assert.assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @Test
    public void testInitialWithSavedFilesIncrementsFilesCounter() throws Exception {
        addFourTestFileToViewModelAndPreferences();
        viewModel.createFileObjects();

        Assert.assertEquals(4, viewModel.journalFilesProperty().size());
    }

    @Test
    public void testRemoveDuplicatesWhenReadingFiles() throws Exception {
        addFourTestFileToViewModelAndPreferences();
        viewModel.createFileObjects();
        viewModel.selectLastJournalFile();

        // should result in 4 real abbreviations and one pseudo abbreviation
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
    }

    @Test
    public void addFileIncreasesCounterOfOpenFilesAndHasNoAbbreviations() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();

        Assert.assertEquals(1, viewModel.journalFilesProperty().size());
        Assert.assertEquals(1, viewModel.abbreviationsProperty().size());
    }

    @Test
    public void addDuplicatedFileResultsInErrorDialog() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.addNewFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    public void testOpenDuplicatedFileResultsInAnException() throws Exception {
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.openFile();
        viewModel.openFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    public void testSelectLastJournalFileSwitchesFilesAndTheirAbbreviations() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Assert.assertEquals(1, viewModel.abbreviationsCountProperty().get());

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Assert.assertEquals(2, viewModel.abbreviationsCountProperty().get());
    }

    @Test
    public void testOpenValidFileContainsTheSpecificEntryAndEnoughAbbreviations() throws Exception {
        Abbreviation testAbbreviation = new Abbreviation("Test Entry", "TE");
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();

        Assert.assertEquals(1, viewModel.journalFilesProperty().size());
        // our test file has 3 abbreviations and one pseudo abbreviation
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    public void testRemoveLastListSetsCurrentFileAndCurrentAbbreviationToNull() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.removeCurrentFile();

        Assert.assertEquals(0, viewModel.journalFilesProperty().size());
        Assert.assertEquals(0, viewModel.abbreviationsProperty().size());
        Assert.assertNull(viewModel.currentFileProperty().get());
        Assert.assertNull(viewModel.currentAbbreviationProperty().get());
    }

    @Test
    public void testMixedFileUsage() throws Exception {
        Abbreviation testAbbreviation = new Abbreviation("Entry", "E");
        Abbreviation testAbbreviation2 = new Abbreviation("EntryEntry", "EE");

        // simulate open file button twice
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(1));

        // size of the list of journal files should be incremented by two
        Assert.assertEquals(2, viewModel.journalFilesProperty().size());
        // our second test file has 4 abbreviations
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        // simulate add new file button
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(2));

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(3, viewModel.journalFilesProperty().size());
        // a new file has zero abbreviations
        Assert.assertEquals(1, viewModel.abbreviationsProperty().size());

        // simulate open file button
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(3));

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(4, viewModel.journalFilesProperty().size());

        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation2)));
    }

    @Test
    public void testBuiltInListsIncludeAllBuiltInAbbreviations() {
        when(abbreviationPreferences.useIEEEAbbreviations()).thenReturn(false);
        viewModel.addBuiltInLists();
        Assert.assertEquals(2, viewModel.journalFilesProperty().getSize());
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(0));
        ObservableList<Abbreviation> expected = FXCollections
                .observableArrayList(JournalAbbreviationLoader.getBuiltInAbbreviations());
        ObservableList<Abbreviation> actualAbbreviations = FXCollections
                .observableArrayList(viewModel.abbreviationsProperty().stream()
                        .map(AbbreviationViewModel::getAbbreviationObject).collect(Collectors.toList()));

        Assert.assertEquals(expected, actualAbbreviations);
    }

    @Test
    public void testBuiltInListsStandardIEEEIncludesAllBuiltIEEEAbbreviations() throws Exception {
        when(abbreviationPreferences.useIEEEAbbreviations()).thenReturn(true);
        viewModel.addBuiltInLists();
        viewModel.selectLastJournalFile();
        Assert.assertEquals(2, viewModel.journalFilesProperty().getSize());
        ObservableList<Abbreviation> expected = FXCollections
                .observableArrayList(JournalAbbreviationLoader.getOfficialIEEEAbbreviations());
        ObservableList<Abbreviation> actualAbbreviations = FXCollections
                .observableArrayList(viewModel.abbreviationsProperty().stream()
                        .map(AbbreviationViewModel::getAbbreviationObject).collect(Collectors.toList()));

        Assert.assertEquals(expected, actualAbbreviations);
    }

    @Test
    public void testcurrentFilePropertyChangeActiveFile() throws Exception {
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
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());

        viewModel.currentFileProperty().set(test1);

        // test if the current abbreviations matches with the ones in testFile1Entries
        Assert.assertEquals(2, viewModel.abbreviationsProperty().size());

        viewModel.currentFileProperty().set(test3);
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test1);
        Assert.assertEquals(2, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test4);
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test5);
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
    }

    @Test
    public void testAddAbbreviationIncludesAbbreviationsInAbbreviationList() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbrevaition(testAbbreviation);

        Assert.assertEquals(6, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    public void testAddDuplicatedAbbreviationResultsInException() throws JabRefException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        viewModel.addAbbreviation("YetAnotherEntry", "YAE");
        viewModel.addAbbreviation("YetAnotherEntry", "YAE");
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    public void testEditSameAbbreviationWithNoChangeDoesNotResultInException() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbrevaition(testAbbreviation);
        editAbbreviation(testAbbreviation);

        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    public void testEditAbbreviationIncludesNewAbbreviationInAbbreviationsList() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        editAbbreviation(testAbbreviation);

        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        editAbbreviation(testAbbreviation);

        Assert.assertEquals(1, viewModel.abbreviationsProperty().size());
        Assert.assertFalse(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    public void testEditAbbreviationToExistingOneResultsInException() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("YetAnotherEntry", "YAE");
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(2));
        viewModel.editAbbreviation("YetAnotherEntry", "YAE");
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    public void testEditAbbreviationToEmptyNameResultsInException() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("", "YAE");
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @Test
    public void testEditAbbreviationToEmptyAbbreviationResultsInException() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("YetAnotherEntry", "");
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @Test
    public void testDeleteAbbreviationSelectsPreviousOne() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbrevaition(testAbbreviation);

        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
        Assert.assertEquals(new AbbreviationViewModel(testAbbreviation), viewModel.currentAbbreviationProperty().get());

        viewModel.deleteAbbreviation();

        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        // check if the previous (the last) element is the current abbreviation
        Assert.assertEquals(viewModel.currentAbbreviationProperty().get(), viewModel.abbreviationsProperty().get(4));
    }

    @Test
    public void testDeleteAbbreviationSelectsNextOne() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile1Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbrevaition(testAbbreviation);
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(1));
        viewModel.deleteAbbreviation();

        Assert.assertEquals(new AbbreviationViewModel(testAbbreviation), viewModel.currentAbbreviationProperty().get());
    }

    @Test
    public void testSaveAbbreviationsToFilesCreatesNewFilesWithWrittenAbbreviations() throws Exception {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile4Entries));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        Abbreviation testAbbreviation = new Abbreviation("JabRefTestEntry", "JTE");
        editAbbreviation(testAbbreviation);

        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile5EntriesWithDuplicate));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        viewModel.deleteAbbreviation();
        Abbreviation testAbbreviation1 = new Abbreviation("SomeOtherEntry", "SOE");
        addAbbrevaition(testAbbreviation1);

        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation1)));

        viewModel.saveJournalAbbreviationFiles();
        String expected = "Abbreviations = Abb" + NEWLINE + "Test Entry = TE" + NEWLINE + "MoreEntries = ME" + NEWLINE
                + "JabRefTestEntry = JTE" + NEWLINE + "";
        String actual = Files.contentOf(testFile4Entries.toFile(), StandardCharsets.UTF_8);

        Assert.assertEquals(expected, actual);

        expected = "EntryEntry = EE" + NEWLINE + "Abbreviations = Abb" + NEWLINE + "Test Entry = TE" + NEWLINE
                + "SomeOtherEntry = SOE" + NEWLINE + "";
        actual = Files.contentOf(testFile5EntriesWithDuplicate.toFile(), StandardCharsets.UTF_8);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSaveExternalFilesListToPreferences() throws Exception {
        addFourTestFileToViewModelAndPreferences();
        List<String> expected = Stream.of(testFile1Entries, testFile3Entries, testFile4Entries, testFile5EntriesWithDuplicate)
                .map(Path::toString).collect(Collectors.toList());
        verify(abbreviationPreferences).setExternalJournalLists(expected);
    }

    private Path createTemporaryTestFile(String name, String content) throws Exception {
        File testFile = tempFolder.newFile(name);
        try (OutputStream outputStream = new FileOutputStream(testFile)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return testFile.toPath();
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
        viewModel.saveEverythingAndUpdateAutoCompleter();
    }

    /**
     * Select the last abbreviation in the list of abbreviations
     */
    private void selectLastAbbreviation() {
        viewModel.currentAbbreviationProperty()
                .set(viewModel.abbreviationsProperty().get(viewModel.abbreviationsCountProperty().get() - 1));
    }
}
