/*  Copyright (C) 2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.journals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefException;
import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.journals.DuplicatedJournalAbbreviationException;
import net.sf.jabref.logic.journals.DuplicatedJournalFileException;
import net.sf.jabref.logic.journals.EmptyFieldException;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.preferences.JabRefPreferences;

import org.assertj.core.util.Files;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * This is the test class of the ManageAbbreviationsViewModel which is used to
 * create the underlying logic of the Manage Abbreviations Dialog UI.
 */
public class ManageJournalAbbreviationsTest {

    private ManageJournalAbbreviationsViewModel viewModel;
    private final String NEWLINE = OS.NEWLINE;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private String emptyTestFile;
    private String testFile1Entries;
    private String testFile3Entries;
    private String testFile4Entries;
    private String testFile5EntriesWithDuplicate;

    private final static JabRefPreferences backupPreferences = Globals.prefs;


    @BeforeClass
    public static void initialize() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Before
    public void setUpViewModel() throws IOException {
        // use a new view model for each test so the test methods won't interfere with one another
        Globals.prefs.putStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS, null);
        viewModel = new ManageJournalAbbreviationsViewModel();
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

    @AfterClass
    public static void tearDown() {
        Globals.prefs = backupPreferences;
    }

    @Test
    public void testInitialHasNoFilesAndNoAbbreviations() {
        Assert.assertEquals(viewModel.journalFilesProperty().size(), 0);
        Assert.assertEquals(viewModel.abbreviationsProperty().size(), 0);
    }

    @Test
    public void testInitialWithSavedFilesIncrementsFilesCounter() throws DuplicatedJournalFileException {
        addFourTestFileToViewModelAndPreferences();
        viewModel = new ManageJournalAbbreviationsViewModel();
        viewModel.createFileObjects();

        Assert.assertEquals(4, viewModel.journalFilesProperty().size());
    }

    @Test
    public void testRemoveDuplicatesWhenReadingFiles() throws DuplicatedJournalFileException {
        addFourTestFileToViewModelAndPreferences();
        viewModel = new ManageJournalAbbreviationsViewModel();
        viewModel.createFileObjects();
        viewModel.selectLastJournalFile();

        // should result in 4 real abbreviations and one pseudo abbreviation
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
    }

    @Test
    public void addFileIncreasesCounterOfOpenFilesAndHasNoAbbreviations() throws DuplicatedJournalFileException {
        viewModel.addNewFile("newTestFile.txt");

        Assert.assertEquals(1, viewModel.journalFilesProperty().size());
        Assert.assertEquals(1, viewModel.abbreviationsProperty().size());
    }

    @Test(expected = DuplicatedJournalFileException.class)
    public void testAddDuplicatedFileResultsInAnException() throws DuplicatedJournalFileException {
        viewModel.addNewFile(testFile1Entries);
        viewModel.addNewFile(testFile1Entries);
    }

    @Test(expected = DuplicatedJournalFileException.class)
    public void testOpenDuplicatedFileResultsInAnException() throws DuplicatedJournalFileException {
        viewModel.openFile(testFile1Entries);
        viewModel.openFile(testFile1Entries);
    }

    @Test
    public void testSelectLastJournalFileSwitchesFilesAndTheirAbbreviations() throws DuplicatedJournalFileException {
        viewModel.addNewFile(emptyTestFile);
        viewModel.selectLastJournalFile();
        Assert.assertEquals(1, viewModel.abbreviationsCountProperty().get());
        viewModel.addNewFile(testFile1Entries);
        viewModel.selectLastJournalFile();
        Assert.assertEquals(2, viewModel.abbreviationsCountProperty().get());
        viewModel.addNewFile(testFile3Entries);
        viewModel.selectLastJournalFile();
        Assert.assertEquals(4, viewModel.abbreviationsCountProperty().get());
        viewModel.addNewFile(testFile4Entries);
        viewModel.selectLastJournalFile();
        Assert.assertEquals(5, viewModel.abbreviationsCountProperty().get());
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        viewModel.selectLastJournalFile();
        Assert.assertEquals(5, viewModel.abbreviationsCountProperty().get());
    }

    @Test
    public void testOpenValidFileContainsTheSpecificEntryAndEnoughAbbreviations()
            throws DuplicatedJournalFileException {
        Abbreviation testAbbreviation = new Abbreviation("Test Entry", "TE");
        viewModel.addNewFile(testFile3Entries);
        viewModel.selectLastJournalFile();

        Assert.assertEquals(1, viewModel.journalFilesProperty().size());
        // our test file has 3 abbreviations and one pseudo abbreviation
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    public void testRemoveLastListSetsCurrentFileAndCurrentAbbreviationToNull() throws DuplicatedJournalFileException {
        viewModel.addNewFile(testFile1Entries);
        viewModel.removeCurrentFile();

        Assert.assertEquals(0, viewModel.journalFilesProperty().size());
        Assert.assertEquals(0, viewModel.abbreviationsProperty().size());
        Assert.assertNull(viewModel.currentFileProperty().get());
        Assert.assertNull(viewModel.currentAbbreviationProperty().get());
    }

    @Test
    public void testMixedFileUsage() throws DuplicatedJournalFileException {
        Abbreviation testAbbreviation = new Abbreviation("Entry", "E");
        Abbreviation testAbbreviation2 = new Abbreviation("EntryEntry", "EE");

        // simulate open file button twice
        viewModel.addNewFile(testFile3Entries);
        viewModel.addNewFile(testFile4Entries);
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(1));

        // size of the list of journal files should be incremented by two
        Assert.assertEquals(2, viewModel.journalFilesProperty().size());
        // our second test file has 4 abbreviations
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        // simulate add new file button
        viewModel.addNewFile("newTestFile.txt");
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(2));

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(3, viewModel.journalFilesProperty().size());
        // a new file has zero abbreviations
        Assert.assertEquals(1, viewModel.abbreviationsProperty().size());

        // simulate open file button
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(3));

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(4, viewModel.journalFilesProperty().size());

        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation2)));
    }

    @Test
    public void testBuiltInListsIncludeAllBuiltInAbbreviations() {
        Globals.prefs.put(JabRefPreferences.USE_IEEE_ABRV, "false");
        viewModel.addBuiltInLists();
        Assert.assertEquals(2, viewModel.journalFilesProperty().getSize());
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(0));
        ObservableList<Abbreviation> expected = FXCollections
                .observableArrayList(Globals.journalAbbreviationLoader.getBuiltInAbbreviations());
        ObservableList<Abbreviation> actualAbbreviations = FXCollections
                .observableArrayList(viewModel.abbreviationsProperty().stream()
                        .map(abbViewModel -> abbViewModel.getAbbreviationObject()).collect(Collectors.toList()));

        Assert.assertEquals(expected, actualAbbreviations);
    }

    @Test
    public void testBuiltInListsStandardIEEEIncludesAllBuiltIEEEAbbreviations() {
        Globals.prefs.put(JabRefPreferences.USE_IEEE_ABRV, "true");
        viewModel.addBuiltInLists();
        viewModel.selectLastJournalFile();
        Assert.assertEquals(2, viewModel.journalFilesProperty().getSize());
        ObservableList<Abbreviation> expected = FXCollections
                .observableArrayList(Globals.journalAbbreviationLoader.getOfficialIEEEAbbreviations());
        ObservableList<Abbreviation> actualAbbreviations = FXCollections
                .observableArrayList(viewModel.abbreviationsProperty().stream()
                        .map(abbViewModel -> abbViewModel.getAbbreviationObject()).collect(Collectors.toList()));

        Assert.assertEquals(expected, actualAbbreviations);
    }

    @Test
    public void testcurrentFilePropertyChangeActiveFile() throws DuplicatedJournalFileException {
        viewModel.addNewFile(testFile1Entries);
        viewModel.addNewFile(testFile3Entries);
        viewModel.addNewFile(testFile4Entries);
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
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
    public void testAddAbbreviationIncludesAbbreviationsInAbbreviationList() throws JabRefException {
        viewModel.addNewFile(testFile4Entries);
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbrevaition(testAbbreviation);

        Assert.assertEquals(6, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test(expected = JabRefException.class)
    public void testAddDuplicatedAbbreviationResultsInException() throws JabRefException {
        viewModel.addNewFile(testFile3Entries);
        viewModel.selectLastJournalFile();
        viewModel.addAbbreviation("YetAnotherEntry", "YAE");
        viewModel.addAbbreviation("YetAnotherEntry", "YAE");
    }

    @Test
    public void testEditSameAbbreviationWithNoChangeDoesNotResultInException() throws JabRefException {
        viewModel.addNewFile(emptyTestFile);
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbrevaition(testAbbreviation);
        editAbbreviation(testAbbreviation);

        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    public void testEditAbbreviationIncludesNewAbbreviationInAbbreviationsList() throws JabRefException {
        viewModel.addNewFile(testFile4Entries);
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        editAbbreviation(testAbbreviation);

        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        viewModel.addNewFile(emptyTestFile);
        viewModel.selectLastJournalFile();
        editAbbreviation(testAbbreviation);

        Assert.assertEquals(1, viewModel.abbreviationsProperty().size());
        Assert.assertFalse(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test(expected = DuplicatedJournalAbbreviationException.class)
    public void testEditAbbreviationToExistingOneResultsInException()
            throws EmptyFieldException, DuplicatedJournalAbbreviationException, DuplicatedJournalFileException {
        viewModel.addNewFile(testFile3Entries);
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("YetAnotherEntry", "YAE");
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(2));
        viewModel.editAbbreviation("YetAnotherEntry", "YAE");
    }

    @Test(expected = EmptyFieldException.class)
    public void testEditAbbreviationToEmptyNameResultsInException()
            throws EmptyFieldException, DuplicatedJournalAbbreviationException, DuplicatedJournalFileException {
        viewModel.addNewFile(testFile3Entries);
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("", "YAE");
    }

    @Test(expected = EmptyFieldException.class)
    public void testEditAbbreviationToEmptyAbbreviationResultsInException()
            throws DuplicatedJournalFileException, EmptyFieldException, DuplicatedJournalAbbreviationException {
        viewModel.addNewFile(testFile3Entries);
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation("YetAnotherEntry", "");
    }

    @Test
    public void testDeleteAbbreviationSelectsPreviousOne() throws JabRefException {
        viewModel.addNewFile(testFile4Entries);
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
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
    public void testDeleteAbbreviationSelectsNextOne() throws JabRefException {
        viewModel.addNewFile(testFile1Entries);
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbrevaition(testAbbreviation);
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(1));
        viewModel.deleteAbbreviation();

        Assert.assertEquals(new AbbreviationViewModel(testAbbreviation), viewModel.currentAbbreviationProperty().get());
    }

    @Test
    public void testSaveAbbreviationsToFilesCreatesNewFilesWithWrittenAbbreviations() throws JabRefException {
        viewModel.addNewFile(testFile4Entries);
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        Abbreviation testAbbreviation = new Abbreviation("JabRefTestEntry", "JTE");
        editAbbreviation(testAbbreviation);

        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        viewModel.addNewFile(testFile5EntriesWithDuplicate);
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
        String actual = Files.contentOf(new File(testFile4Entries), StandardCharsets.UTF_8);

        Assert.assertEquals(expected, actual);

        expected = "Abbreviations = Abb" + NEWLINE + "Test Entry = TE" + NEWLINE + "MoreEntries = ME" + NEWLINE
                + "SomeOtherEntry = SOE" + NEWLINE + "";
        actual = Files.contentOf(new File(testFile5EntriesWithDuplicate), StandardCharsets.UTF_8);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSaveExternalFilesListToPreferences() throws DuplicatedJournalFileException {
        addFourTestFileToViewModelAndPreferences();
        List<String> expected = Arrays.asList(testFile1Entries, testFile3Entries, testFile4Entries,
                testFile5EntriesWithDuplicate);
        List<String> actual = Globals.prefs.getStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS);

        Assert.assertEquals(expected, actual);
    }

    private String createTemporaryTestFile(String name, String content) throws IOException {
        File testFile = null;
        testFile = tempFolder.newFile(name);
        try (OutputStream outputStream = new FileOutputStream(testFile)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return testFile.getAbsolutePath();
    }

    private void addAbbrevaition(Abbreviation testAbbreviation) throws DuplicatedJournalAbbreviationException {
        viewModel.addAbbreviation(testAbbreviation.getName(), testAbbreviation.getAbbreviation());
    }

    private void editAbbreviation(Abbreviation testAbbreviation) throws JabRefException {
        viewModel.editAbbreviation(testAbbreviation.getName(), testAbbreviation.getAbbreviation());
    }

    private void addFourTestFileToViewModelAndPreferences() throws DuplicatedJournalFileException {
        viewModel.addNewFile(testFile1Entries);
        viewModel.addNewFile(testFile3Entries);
        viewModel.addNewFile(testFile4Entries);
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        viewModel.saveExternalFilesList();
    }

    /**
     * Select the last abbreviation in the list of abbreviations
     */
    private void selectLastAbbreviation() {
        viewModel.currentAbbreviationProperty()
                .set(viewModel.abbreviationsProperty().get(viewModel.abbreviationsCountProperty().get() - 1));
    }

}
