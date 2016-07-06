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

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefException;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.journals.Abbreviation;

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
 *
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
    public void testInitialWithExternalFiles() {
        // saves 4 test files to the list of external files
        saveFilesToPreferences();

        viewModel = new ManageJournalAbbreviationsViewModel();
        viewModel.createFileObjects();

        // check whether all external files from the preferences will
        // be read when creating the view model
        Assert.assertEquals(4, viewModel.journalFilesProperty().size());
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(3));
        // remove duplicated entry
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
    }

    @Test
    public void testAddFile() {
        int size = viewModel.journalFilesProperty().size();

        // simulate add new file button
        savelyAddNewFileToViewModel("newTestFile.txt");

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(size + 1, viewModel.journalFilesProperty().size());
        // no abbreviations should be present when creating a new file
        Assert.assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @Test(expected = JabRefException.class)
    public void testAddDuplicatedFile() throws JabRefException {
        viewModel.addNewFile(testFile1Entries);
        viewModel.addNewFile(testFile1Entries);
    }

    @Test(expected = JabRefException.class)
    public void testOpenDuplicatedFile() throws JabRefException {
        viewModel.openFile(testFile1Entries);
        viewModel.openFile(testFile1Entries);
    }

    @Test
    public void testOpenValidFilesWithAbbreviations() {
        savelyAddNewFileToViewModel(emptyTestFile);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(0));
        Assert.assertEquals(0, viewModel.abbreviationsCountProperty().get());
        savelyAddNewFileToViewModel(testFile1Entries);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(1));
        Assert.assertEquals(1, viewModel.abbreviationsCountProperty().get());
        savelyAddNewFileToViewModel(testFile3Entries);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(2));
        Assert.assertEquals(3, viewModel.abbreviationsCountProperty().get());
        savelyAddNewFileToViewModel(testFile4Entries);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(3));
        Assert.assertEquals(4, viewModel.abbreviationsCountProperty().get());
        savelyAddNewFileToViewModel(testFile5EntriesWithDuplicate);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(4));
        Assert.assertEquals(4, viewModel.abbreviationsCountProperty().get());
    }

    @Test
    public void testOpenValidFile() {
        int size = viewModel.journalFilesProperty().size();
        Abbreviation testAbbreviation = new Abbreviation("Test Entry", "TE");

        // simulate open file button
        savelyAddNewFileToViewModel(testFile3Entries);
        size++;
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(0));

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(size, viewModel.journalFilesProperty().size());
        // our test file has 3 abbreviations
        Assert.assertEquals(3, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(testAbbreviation));
    }

    @Test
    public void testRemoveList() {
        savelyAddNewFileToViewModel(testFile1Entries);
        savelyAddNewFileToViewModel(testFile3Entries);
        savelyAddNewFileToViewModel(testFile4Entries);
        savelyAddNewFileToViewModel(testFile5EntriesWithDuplicate);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(3));

        Assert.assertEquals(4, viewModel.journalFilesProperty().size());
        Assert.assertEquals(4, viewModel.abbreviationsCountProperty().get());

        viewModel.removeCurrentList();

        Assert.assertEquals(3, viewModel.journalFilesProperty().size());
        Assert.assertEquals(4, viewModel.abbreviationsCountProperty().get());

        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(0));
        viewModel.removeCurrentList();

        Assert.assertEquals(2, viewModel.journalFilesProperty().size());
        Assert.assertEquals(3, viewModel.abbreviationsCountProperty().get());

        viewModel.removeCurrentList();
        viewModel.removeCurrentList();

        Assert.assertEquals(0, viewModel.journalFilesProperty().size());
        Assert.assertEquals(0, viewModel.abbreviationsProperty().size());
        Assert.assertNull(viewModel.currentFileProperty().get());
        Assert.assertNull(viewModel.currentAbbreviationProperty().get());
    }

    @Test
    public void testMixedFileUsage() {
        int size = viewModel.journalFilesProperty().size();
        Abbreviation testAbbreviation = new Abbreviation("Entry", "E");
        Abbreviation testAbbreviation2 = new Abbreviation("EntryEntry", "EE");

        // simulate open file button twice
        savelyAddNewFileToViewModel(testFile3Entries);
        size++;
        savelyAddNewFileToViewModel(testFile4Entries);
        size++;
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(1));

        // size of the list of journal files should be incremented by two
        Assert.assertEquals(size, viewModel.journalFilesProperty().size());
        // our second test file has 4 abbreviations
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(testAbbreviation));

        // simulate add new file button
        savelyAddNewFileToViewModel("newTestFile.txt");
        size++;
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(2));

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(size, viewModel.journalFilesProperty().size());
        // a new file has zero abbreviations
        Assert.assertEquals(0, viewModel.abbreviationsProperty().size());

        // simulate open file button
        savelyAddNewFileToViewModel(testFile5EntriesWithDuplicate);
        size++;
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(3));

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(size, viewModel.journalFilesProperty().size());

        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
        // check some abbreviation
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(testAbbreviation2));
    }

    @Test
    public void testOpenEmptyFile() {
        int size = viewModel.journalFilesProperty().size();

        // simulate open file button
        savelyAddNewFileToViewModel(emptyTestFile);
        size++;

        // size of the list of journal files should be incremented by one
        Assert.assertEquals(size, viewModel.journalFilesProperty().size());
        // our test file has 3 abbreviations
        Assert.assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @Test
    public void testChangeActiveFile() {
        savelyAddNewFileToViewModel(testFile1Entries);
        savelyAddNewFileToViewModel(testFile3Entries);
        savelyAddNewFileToViewModel(testFile4Entries);
        savelyAddNewFileToViewModel(testFile5EntriesWithDuplicate);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(3));
        // retrieve object for testFile3Entries with 3 test entries
        AbbreviationsFile test1 = viewModel.journalFilesProperty().get(0);
        AbbreviationsFile test3 = viewModel.journalFilesProperty().get(1);
        AbbreviationsFile test4 = viewModel.journalFilesProperty().get(2);
        AbbreviationsFile test5 = viewModel.journalFilesProperty().get(3);

        // test if the last opened file is active, but duplicated entry has been removed
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());

        // simulate active file has changed
        viewModel.changeActiveFile(test1);

        // test if the current abbreviations matches with the ones in testFile3Entries
        Assert.assertEquals(1, viewModel.abbreviationsProperty().size());

        viewModel.changeActiveFile(test3);
        Assert.assertEquals(3, viewModel.abbreviationsProperty().size());
        viewModel.changeActiveFile(test1);
        Assert.assertEquals(1, viewModel.abbreviationsProperty().size());
        viewModel.changeActiveFile(test4);
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());
        viewModel.changeActiveFile(test5);
        Assert.assertEquals(4, viewModel.abbreviationsProperty().size());

    }

    @Test
    public void testAddAbbreviation() {
        savelyAddNewFileToViewModel(testFile4Entries);
        savelyAddNewFileToViewModel(testFile5EntriesWithDuplicate);
        int size = viewModel.abbreviationsProperty().size();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        savelyAddAbbreviation(testAbbreviation);
        size++;

        Assert.assertEquals(size, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(testAbbreviation));
    }

    @Test(expected = JabRefException.class)
    public void testAddDuplicatedAbbreviation() throws JabRefException {
        savelyAddNewFileToViewModel(testFile3Entries);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(0));
        viewModel.abbreviationsNameProperty().set("YetAnotherEntry");
        viewModel.abbreviationsAbbreviationProperty().set("YAE");
        viewModel.addAbbreviation();
        viewModel.addAbbreviation();
    }

    @Test
    public void testEditSameAbbreviationWithNoChange() {
        savelyAddNewFileToViewModel(emptyTestFile);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(0));
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        savelyAddAbbreviation(testAbbreviation);
        savelyEditAbbreviation(testAbbreviation);
    }

    @Test
    public void testEditAbbreviation() {
        savelyAddNewFileToViewModel(testFile4Entries);
        savelyAddNewFileToViewModel(testFile5EntriesWithDuplicate);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(1));
        int size = viewModel.abbreviationsProperty().size();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        savelyEditAbbreviation(testAbbreviation);

        Assert.assertEquals(size, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(testAbbreviation));

        savelyAddNewFileToViewModel(emptyTestFile);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(2));
        savelyEditAbbreviation(testAbbreviation);

        Assert.assertEquals(0, viewModel.abbreviationsProperty().size());
        Assert.assertFalse(viewModel.abbreviationsProperty().contains(testAbbreviation));
    }

    @Test(expected = JabRefException.class)
    public void testEditAbbreviationToExistingOne() throws JabRefException {
        savelyAddNewFileToViewModel(testFile3Entries);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(0));
        Assert.assertEquals(3, viewModel.abbreviationsProperty().size());
        viewModel.abbreviationsNameProperty().set("YetAnotherEntry");
        viewModel.abbreviationsAbbreviationProperty().set("YAE");
        viewModel.editAbbreviation();
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(1));
        viewModel.editAbbreviation();
    }

    @Test
    public void testDeleteAbbreviationSelectPrevious() {
        savelyAddNewFileToViewModel(testFile4Entries);
        savelyAddNewFileToViewModel(testFile5EntriesWithDuplicate);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(1));
        int size = viewModel.abbreviationsProperty().size();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        savelyAddAbbreviation(testAbbreviation);

        Assert.assertTrue(viewModel.abbreviationsProperty().contains(testAbbreviation));
        Assert.assertEquals(testAbbreviation, viewModel.currentAbbreviationProperty().get());

        viewModel.deleteAbbreviation();

        Assert.assertEquals(size, viewModel.abbreviationsProperty().size());
        // check if the previous (the last) element is the current abbreviation
        Assert.assertEquals(viewModel.currentAbbreviationProperty().get(),
                viewModel.abbreviationsProperty().get(size - 1));
    }

    @Test
    public void testDeleteAbbreviationSelectNext() {
        savelyAddNewFileToViewModel(testFile1Entries);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(0));
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        savelyAddAbbreviation(testAbbreviation);

        // simulate select first entry
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(0));
        viewModel.deleteAbbreviation();

        Assert.assertEquals(testAbbreviation, viewModel.currentAbbreviationProperty().get());
    }

    @Test
    public void testDeleteAbbrevaitionSelectNull() {
        savelyAddNewFileToViewModel(testFile1Entries);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(0));
        viewModel.deleteAbbreviation();
        Assert.assertNull(viewModel.currentAbbreviationProperty().get());
    }

    @Test
    public void testSaveAbbreviationsToFiles() {
        savelyAddNewFileToViewModel(testFile4Entries);
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(0));
        int size = viewModel.abbreviationsProperty().size();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        savelyEditAbbreviation(testAbbreviation);

        Assert.assertEquals(size, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(testAbbreviation));

        savelyAddNewFileToViewModel(testFile5EntriesWithDuplicate);
        size = viewModel.abbreviationsProperty().size();
        viewModel.changeActiveFile(viewModel.journalFilesProperty().get(1));
        viewModel.deleteAbbreviation();
        Abbreviation testAbbreviation1 = new Abbreviation("SomeOtherEntry", "SOE");
        savelyAddAbbreviation(testAbbreviation1);

        Assert.assertEquals(size, viewModel.abbreviationsProperty().size());
        Assert.assertTrue(viewModel.abbreviationsProperty().contains(testAbbreviation1));

        viewModel.saveJournalAbbreviationFiles();

        String expected = "YetAnotherEntry = YAE" + Globals.NEWLINE + "Test Entry = TE" + Globals.NEWLINE
                + "MoreEntries = ME" + Globals.NEWLINE + "Entry = E" + Globals.NEWLINE + "";
        String actual = Files.contentOf(new File(testFile4Entries), StandardCharsets.UTF_8);
        Assert.assertEquals(expected, actual);

        expected = "Test Entry = TE" + Globals.NEWLINE + "MoreEntries = ME" + Globals.NEWLINE + "EntryEntry = EE"
                + Globals.NEWLINE + "SomeOtherEntry = SOE" + Globals.NEWLINE + "";
        actual = Files.contentOf(new File(testFile5EntriesWithDuplicate), StandardCharsets.UTF_8);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSaveExternalFilesListToPreferences() {
        saveFilesToPreferences();

        List<String> expected = Arrays.asList(testFile1Entries, testFile3Entries, testFile4Entries,
                testFile5EntriesWithDuplicate);
        List<String> actual = Globals.prefs.getStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBindings() {
        AbbreviationsFile testFile = new AbbreviationsFile(testFile5EntriesWithDuplicate);
        try {
            testFile.readAbbreviations();
        } catch (FileNotFoundException e) {
            Assert.fail(e.getLocalizedMessage());
        }
        SimpleListProperty<AbbreviationsFile> testProperty = new SimpleListProperty<>(
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

    private void addAbbrevaition(Abbreviation testAbbreviation) throws JabRefException {
        viewModel.abbreviationsNameProperty().set(testAbbreviation.getName());
        viewModel.abbreviationsAbbreviationProperty().set(testAbbreviation.getAbbreviation());
        viewModel.addAbbreviation();
    }

    private void editAbbreviation(Abbreviation testAbbreviation) throws JabRefException {
        viewModel.abbreviationsNameProperty().set(testAbbreviation.getName());
        viewModel.abbreviationsAbbreviationProperty().set(testAbbreviation.getAbbreviation());
        viewModel.editAbbreviation();
    }

    private void saveFilesToPreferences() {
        savelyAddNewFileToViewModel(testFile1Entries);
        savelyAddNewFileToViewModel(testFile3Entries);
        savelyAddNewFileToViewModel(testFile4Entries);
        savelyAddNewFileToViewModel(testFile5EntriesWithDuplicate);
        viewModel.saveExternalFilesList();
    }

    /**
     * This will add the given file to the current view model.
     * In case the given file already exists it will fail the current test.
     * This is useful for testing purposes so you don't have to always handle the exceptions.
     *
     * @param fileToAdd to the view model
     */
    private void savelyAddNewFileToViewModel(String path) {
        try {
            viewModel.addNewFile(path);
        } catch (JabRefException e) {
            Assert.fail("The same file has been opened/created multiple times.");
        }
    }

    /**
     * This will add the given abbreviation to the current view model.
     * In case the given abbreviation already exists it will fail the current test.
     * This is useful for testing purposes so you don't have to always handle the exceptions.
     *
     * @param testAbbreviation which will be added to the view model
     */
    private void savelyAddAbbreviation(Abbreviation testAbbreviation) {
        try {
            addAbbrevaition(testAbbreviation);
        } catch (JabRefException e) {
            Assert.fail("The same abbreviation already exists in the current list.");
        }
    }

    /**
     * This will edit the given abbreviation.
     * In case the given abbreviation already exists it will fail the current test.
     * This is useful for testing purposes so you don't have to always handle the exceptions.
     *
     * @param testAbbreviation which will be edited
     */
    private void savelyEditAbbreviation(Abbreviation testAbbreviation) {
        try {
            editAbbreviation(testAbbreviation);
        } catch (JabRefException e1) {
            Assert.fail("The same abbreviation already exists in the current list.");
        }
    }

}
