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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefException;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.journals.DuplicatedJournalAbbreviationException;
import net.sf.jabref.logic.journals.DuplicatedJournalFileException;
import net.sf.jabref.logic.journals.EmptyFieldException;

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
    public void setUpViewModel() {
        // use a new view model for each test so the test methods won't interfere with one another
        Globals.prefs.putStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS, null);
        viewModel = new ManageJournalAbbreviationsViewModel();
        emptyTestFile = createTemporaryTestFile("emptyTestFile.txt", "");
        testFile1Entries = createTemporaryTestFile("testFile1Entries.txt", "Test Entry = TE" + Globals.NEWLINE + "");
        testFile3Entries = createTemporaryTestFile("testFile3Entries.txt", "Abbreviations = Abb" + Globals.NEWLINE
                + "Test Entry = TE" + Globals.NEWLINE + "MoreEntries = ME" + Globals.NEWLINE + "");
        testFile4Entries = createTemporaryTestFile("testFile4Entries.txt",
                "Abbreviations = Abb" + Globals.NEWLINE + "Test Entry = TE" + Globals.NEWLINE + "MoreEntries = ME"
                        + Globals.NEWLINE + "Entry = E" + Globals.NEWLINE + "");
        testFile5EntriesWithDuplicate = createTemporaryTestFile("testFile5Entries.txt",
                "Abbreviations = Abb" + Globals.NEWLINE + "Test Entry = TE" + Globals.NEWLINE + "Test Entry = TE"
                        + Globals.NEWLINE + "MoreEntries = ME" + Globals.NEWLINE + "EntryEntry = EE" + Globals.NEWLINE
                        + "");
    }

    @AfterClass
    public static void tearDown() {
        Globals.prefs = backupPreferences;
    }

    @Test
    public void testInitial() {
        Assert.assertEquals(viewModel.journalFilesProperty().size(), 0);
        Assert.assertEquals(viewModel.abbreviationsProperty().size(), 0);
    }

    @Test
    public void testInitialWithExternalFiles() throws DuplicatedJournalFileException {
        // saves 4 test files to the list of external files
        saveFilesToPreferences();

        viewModel = new ManageJournalAbbreviationsViewModel();
        viewModel.createFileObjects();

        // check whether all external files from the preferences will
        // be read when creating the view model
        Assert.assertEquals(4, viewModel.journalFilesProperty().size());
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(3));
        // remove duplicated entry
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
    }

    @Test
    public void testAddFile() throws DuplicatedJournalFileException {
        int size = viewModel.journalFilesProperty().size();

        // simulate add new file button
        viewModel.addNewFile("newTestFile.txt");

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(size + 1, viewModel.journalFilesProperty().size());
        // no abbreviations should be present when creating a new file
        Assert.assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @Test(expected = DuplicatedJournalFileException.class)
    public void testAddDuplicatedFile() throws DuplicatedJournalFileException {
        viewModel.addNewFile(testFile1Entries);
        viewModel.addNewFile(testFile1Entries);
    }

    @Test(expected = DuplicatedJournalFileException.class)
    public void testOpenDuplicatedFile() throws DuplicatedJournalFileException {
        viewModel.openFile(testFile1Entries);
        viewModel.openFile(testFile1Entries);
    }

    @Test
    public void testOpenValidFilesWithAbbreviations() throws DuplicatedJournalFileException {
        viewModel.addNewFile(emptyTestFile);
        selectLastJournalFile();
        Assert.assertEquals(1, viewModel.abbreviationsCountProperty().get());
        viewModel.addNewFile(testFile1Entries);
        selectLastJournalFile();
        Assert.assertEquals(2, viewModel.abbreviationsCountProperty().get());
        viewModel.addNewFile(testFile3Entries);
        selectLastJournalFile();
        Assert.assertEquals(4, viewModel.abbreviationsCountProperty().get());
        viewModel.addNewFile(testFile4Entries);
        selectLastJournalFile();
        Assert.assertEquals(5, viewModel.abbreviationsCountProperty().get());
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        selectLastJournalFile();
        Assert.assertEquals(5, viewModel.abbreviationsCountProperty().get());
    }

    @Test
    public void testOpenValidFile() throws DuplicatedJournalFileException {
        int size = 0;
        Abbreviation testAbbreviation = new Abbreviation("Test Entry", "TE");

        // simulate open file button
        viewModel.addNewFile(testFile3Entries);
        size++;
        selectLastJournalFile();

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(size, viewModel.journalFilesProperty().size());
        // our test file has 3 abbreviations
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test
    public void testRemoveList() throws DuplicatedJournalFileException {
        viewModel.addNewFile(testFile1Entries);
        viewModel.addNewFile(testFile3Entries);
        viewModel.addNewFile(testFile4Entries);
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        selectLastJournalFile();

        Assert.assertEquals(4, viewModel.journalFilesProperty().size());
        Assert.assertEquals(5, viewModel.abbreviationsCountProperty().get());

        viewModel.removeCurrentList();

        Assert.assertEquals(3, viewModel.journalFilesProperty().size());
        Assert.assertEquals(5, viewModel.abbreviationsCountProperty().get());

        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(0));
        viewModel.removeCurrentList();

        Assert.assertEquals(2, viewModel.journalFilesProperty().size());
        Assert.assertEquals(4, viewModel.abbreviationsCountProperty().get());

        viewModel.removeCurrentList();
        viewModel.removeCurrentList();

        Assert.assertEquals(0, viewModel.journalFilesProperty().size());
        Assert.assertEquals(0, viewModel.abbreviationsProperty().size());
        Assert.assertNull(viewModel.currentFileProperty().get());
        Assert.assertNull(viewModel.currentAbbreviationProperty().get());
    }

    @Test
    public void testMixedFileUsage() throws DuplicatedJournalFileException {
        int size = 0;
        Abbreviation testAbbreviation = new Abbreviation("Entry", "E");
        Abbreviation testAbbreviation2 = new Abbreviation("EntryEntry", "EE");

        // simulate open file button twice
        viewModel.addNewFile(testFile3Entries);
        size++;
        viewModel.addNewFile(testFile4Entries);
        size++;
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(1));

        // size of the list of journal files should be incremented by two
        Assert.assertEquals(size, viewModel.journalFilesProperty().size());
        // our second test file has 4 abbreviations
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        // simulate add new file button
        viewModel.addNewFile("newTestFile.txt");
        size++;
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(2));

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(size, viewModel.journalFilesProperty().size());
        // a new file has zero abbreviations
        Assert.assertEquals(1, viewModel.abbreviationsProperty().size());

        // simulate open file button
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        size++;
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(3));

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(size, viewModel.journalFilesProperty().size());

        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation2)));
    }

    @Test
    public void testBuiltInLists() {
        viewModel.addBuiltInLists();
        Assert.assertEquals(2, viewModel.journalFilesProperty().getSize());
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(0));
        ObservableList<Abbreviation> expected = FXCollections
                .observableArrayList(Globals.journalAbbreviationLoader.getBuiltInAbbreviations());
        ObservableList<AbbreviationViewModel> actual = viewModel.abbreviationsProperty().get();
        ObservableList<Abbreviation> actualAbbreviations = FXCollections.observableArrayList();
        actual.forEach(abbViewModel -> actualAbbreviations.add(abbViewModel.getAbbreviationObject()));
        Assert.assertEquals(expected, actualAbbreviations);
    }

    @Test
    public void testBuiltInListsStandardIEEE() {
        viewModel.addBuiltInLists();
        selectLastJournalFile();
        Globals.prefs.put(JabRefPreferences.USE_IEEE_ABRV, "true");
        Assert.assertEquals(2, viewModel.journalFilesProperty().getSize());
        ObservableList<Abbreviation> expected = FXCollections
                .observableArrayList(Globals.journalAbbreviationLoader.getOfficialIEEEAbbreviations());
        ObservableList<AbbreviationViewModel> actual = viewModel.abbreviationsProperty().get();
        ObservableList<Abbreviation> actualAbbreviations = FXCollections.observableArrayList();
        actual.forEach(abbViewModel -> actualAbbreviations.add(abbViewModel.getAbbreviationObject()));
        Assert.assertEquals(expected, actualAbbreviations);
    }

    @Test
    public void testOpenEmptyFile() throws DuplicatedJournalFileException {
        int size = viewModel.journalFilesProperty().size();

        // simulate open file button
        viewModel.addNewFile(emptyTestFile);
        size++;

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(size, viewModel.journalFilesProperty().size());
        // our test file has 3 abbreviations
        Assert.assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @Test
    public void testChangeActiveFile() throws DuplicatedJournalFileException {
        viewModel.addNewFile(testFile1Entries);
        viewModel.addNewFile(testFile3Entries);
        viewModel.addNewFile(testFile4Entries);
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        selectLastJournalFile();
        // retrieve object for testFile3Entries with 3 test entries
        AbbreviationsFileViewModel test1 = viewModel.journalFilesProperty().get(0);
        AbbreviationsFileViewModel test3 = viewModel.journalFilesProperty().get(1);
        AbbreviationsFileViewModel test4 = viewModel.journalFilesProperty().get(2);
        AbbreviationsFileViewModel test5 = viewModel.journalFilesProperty().get(3);

        // test if the last opened file is active, but duplicated entry has been removed
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());

        // simulate active file has changed
        viewModel.changeActiveFile(test1);

        // test if the current abbreviations matches with the ones in testFile1Entries
        Assert.assertEquals(2, viewModel.abbreviationsProperty().size());

        viewModel.changeActiveFile(test3);
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
        viewModel.changeActiveFile(test1);
        Assert.assertEquals(2, viewModel.abbreviationsProperty().size());
        viewModel.changeActiveFile(test4);
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());
        viewModel.changeActiveFile(test5);
        Assert.assertEquals(5, viewModel.abbreviationsProperty().size());

    }

    @Test
    public void testAddAbbreviation() throws JabRefException {
        viewModel.addNewFile(testFile4Entries);
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        int size = 5;
        selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbrevaition(testAbbreviation);
        size++;

        Assert.assertEquals(size, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test(expected = JabRefException.class)
    public void testAddDuplicatedAbbreviation() throws JabRefException {
        viewModel.addNewFile(testFile3Entries);
        selectLastJournalFile();
        viewModel.abbreviationsNameProperty().set("YetAnotherEntry");
        viewModel.abbreviationsAbbreviationProperty().set("YAE");
        viewModel.addAbbreviation();
        viewModel.addAbbreviation();
    }

    @Test
    public void testEditSameAbbreviationWithNoChange() throws JabRefException {
        viewModel.addNewFile(emptyTestFile);
        selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbrevaition(testAbbreviation);
        editAbbreviation(testAbbreviation);
    }

    @Test
    public void testEditAbbreviation() throws JabRefException {
        viewModel.addNewFile(testFile4Entries);
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        selectLastJournalFile();
        selectLastAbbreviation();
        int size = 5;
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        editAbbreviation(testAbbreviation);

        Assert.assertEquals(size, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        viewModel.addNewFile(emptyTestFile);
        selectLastJournalFile();
        editAbbreviation(testAbbreviation);

        Assert.assertEquals(1, viewModel.abbreviationsProperty().size());
        Assert.assertFalse(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @Test(expected = DuplicatedJournalAbbreviationException.class)
    public void testEditAbbreviationToExistingOne()
            throws EmptyFieldException, DuplicatedJournalAbbreviationException, DuplicatedJournalFileException {
        viewModel.addNewFile(testFile3Entries);
        selectLastJournalFile();
        selectLastAbbreviation();
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
        viewModel.abbreviationsNameProperty().set("YetAnotherEntry");
        viewModel.abbreviationsAbbreviationProperty().set("YAE");
        viewModel.editAbbreviation();
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(2));
        viewModel.editAbbreviation();
    }

    @Test(expected = EmptyFieldException.class)
    public void testEditAbbreviationToEmptyName()
            throws EmptyFieldException, DuplicatedJournalAbbreviationException, DuplicatedJournalFileException {
        viewModel.addNewFile(testFile3Entries);
        selectLastJournalFile();
        selectLastAbbreviation();
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
        viewModel.abbreviationsNameProperty().set("");
        viewModel.abbreviationsAbbreviationProperty().set("YAE");
        viewModel.editAbbreviation();
    }

    @Test(expected = EmptyFieldException.class)
    public void testEditAbbreviationToEmptyAbbreviation()
            throws DuplicatedJournalFileException, EmptyFieldException, DuplicatedJournalAbbreviationException {
        viewModel.addNewFile(testFile3Entries);
        selectLastJournalFile();
        selectLastAbbreviation();
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
        viewModel.abbreviationsNameProperty().set("YetAnotherEntry");
        viewModel.abbreviationsAbbreviationProperty().set("");
        viewModel.editAbbreviation();
    }

    @Test
    public void testDeleteAbbreviationSelectPrevious() throws JabRefException {
        viewModel.addNewFile(testFile4Entries);
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        selectLastJournalFile();
        int size = 5;
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbrevaition(testAbbreviation);

        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
        Assert.assertEquals(new AbbreviationViewModel(testAbbreviation), viewModel.currentAbbreviationProperty().get());

        viewModel.deleteAbbreviation();

        Assert.assertEquals(size, viewModel.abbreviationsProperty().size());
        // check if the previous (the last) element is the current abbreviation
        Assert.assertEquals(viewModel.currentAbbreviationProperty().get(),
                viewModel.abbreviationsProperty().get(size - 1));
    }

    @Test
    public void testDeleteAbbreviationSelectNext() throws JabRefException {
        viewModel.addNewFile(testFile1Entries);
        selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        addAbbrevaition(testAbbreviation);

        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(1));
        viewModel.deleteAbbreviation();

        Assert.assertEquals(new AbbreviationViewModel(testAbbreviation), viewModel.currentAbbreviationProperty().get());
    }

    @Test
    public void testSaveAbbreviationsToFiles() throws JabRefException {
        viewModel.addNewFile(testFile4Entries);
        selectLastJournalFile();
        selectLastAbbreviation();
        int size = 5;
        Abbreviation testAbbreviation = new Abbreviation("JabRefTestEntry", "JTE");
        editAbbreviation(testAbbreviation);
        Assert.assertEquals(size, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        selectLastJournalFile();
        selectLastAbbreviation();
        size = viewModel.abbreviationsProperty().size();
        viewModel.deleteAbbreviation();
        Abbreviation testAbbreviation1 = new Abbreviation("SomeOtherEntry", "SOE");
        addAbbrevaition(testAbbreviation1);

        Assert.assertEquals(size, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation1)));

        viewModel.saveJournalAbbreviationFiles();

        String expected = "Abbreviations = Abb" + Globals.NEWLINE + "Test Entry = TE" + Globals.NEWLINE
                + "MoreEntries = ME" + Globals.NEWLINE + "JabRefTestEntry = JTE" + Globals.NEWLINE + "";
        String actual = Files.contentOf(new File(testFile4Entries), StandardCharsets.UTF_8);
        Assert.assertEquals(expected, actual);

        expected = "Abbreviations = Abb" + Globals.NEWLINE + "Test Entry = TE" + Globals.NEWLINE + "MoreEntries = ME"
                + Globals.NEWLINE + "SomeOtherEntry = SOE" + Globals.NEWLINE + "";
        actual = Files.contentOf(new File(testFile5EntriesWithDuplicate), StandardCharsets.UTF_8);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSaveExternalFilesListToPreferences() throws DuplicatedJournalFileException {
        saveFilesToPreferences();

        List<String> expected = Arrays.asList(testFile1Entries, testFile3Entries, testFile4Entries,
                testFile5EntriesWithDuplicate);
        List<String> actual = Globals.prefs.getStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBindings() throws FileNotFoundException {
        AbbreviationsFileViewModel testFile = new AbbreviationsFileViewModel(testFile5EntriesWithDuplicate);
        testFile.readAbbreviations();
        SimpleListProperty<AbbreviationsFileViewModel> testProperty = new SimpleListProperty<>(
                FXCollections.observableArrayList(testFile));

        viewModel.bindFileItems(testProperty);

        Assert.assertEquals(testFile, viewModel.currentFileProperty().get());
    }

    private String createTemporaryTestFile(String name, String content) {
        File testFile = null;
        try {
            testFile = tempFolder.newFile(name);
            try (OutputStream outputStream = new FileOutputStream(testFile)) {
                outputStream.write(content.getBytes());
            }
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
        return testFile.getAbsolutePath();
    }

    private void addAbbrevaition(Abbreviation testAbbreviation) throws DuplicatedJournalAbbreviationException {
        viewModel.abbreviationsNameProperty().set(testAbbreviation.getName());
        viewModel.abbreviationsAbbreviationProperty().set(testAbbreviation.getAbbreviation());
        viewModel.addAbbreviation();
    }

    private void editAbbreviation(Abbreviation testAbbreviation) throws JabRefException {
        viewModel.abbreviationsNameProperty().set(testAbbreviation.getName());
        viewModel.abbreviationsAbbreviationProperty().set(testAbbreviation.getAbbreviation());
        viewModel.editAbbreviation();
    }

    private void saveFilesToPreferences() throws DuplicatedJournalFileException {
        viewModel.addNewFile(testFile1Entries);
        viewModel.addNewFile(testFile3Entries);
        viewModel.addNewFile(testFile4Entries);
        viewModel.addNewFile(testFile5EntriesWithDuplicate);
        viewModel.saveExternalFilesList();
    }

    /**
     * Select the most recently added journal abbreviation file
     */
    private void selectLastJournalFile() {
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(viewModel.journalFilesProperty().size() - 1));
    }

    /**
     * Select the last abbreviation in the list of abbreviations
     */
    private void selectLastAbbreviation() {
        viewModel.currentAbbreviationProperty()
                .set(viewModel.abbreviationsProperty().get(viewModel.abbreviationsCountProperty().get() - 1));
    }

}
