package net.sf.jabref.gui.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.ImportDataTest;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;
import net.sf.jabref.testutils.category.GUITests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(GUITests.class)
public class EntryFromFileCreatorManagerTest {

    // Needed to initialize ExternalFileTypes
    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testGetCreator() {
        EntryFromFileCreatorManager manager = new EntryFromFileCreatorManager();
        EntryFromFileCreator creator = manager.getEntryCreator(ImportDataTest.NOT_EXISTING_PDF);
        Assert.assertNull(creator);

        creator = manager.getEntryCreator(ImportDataTest.FILE_IN_DATABASE);
        Assert.assertNotNull(creator);
        Assert.assertTrue(creator.accept(ImportDataTest.FILE_IN_DATABASE));
    }

    @Test
    @Ignore
    public void testAddEntrysFromFiles() throws FileNotFoundException, IOException {
        try (FileInputStream stream = new FileInputStream(ImportDataTest.UNLINKED_FILES_TEST_BIB);
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(Globals.prefs.getImportFormatPreferences()).parse(reader);
            BibDatabase database = result.getDatabase();

            List<File> files = new ArrayList<>();

            files.add(ImportDataTest.FILE_NOT_IN_DATABASE);
            files.add(ImportDataTest.NOT_EXISTING_PDF);

            EntryFromFileCreatorManager manager = new EntryFromFileCreatorManager();
            List<String> errors = manager.addEntrysFromFiles(files, database, null, true);

            /**
             * One file doesn't exist, so adding it as an entry should lead to an error message.
             */
            Assert.assertEquals(1, errors.size());

            boolean file1Found = false;
            boolean file2Found = false;
            for (BibEntry entry : database.getEntries()) {
                String filesInfo = entry.getField("file").get();
                if (filesInfo.contains(files.get(0).getName())) {
                    file1Found = true;
                }
                if (filesInfo.contains(files.get(1).getName())) {
                    file2Found = true;
                }
            }

            Assert.assertTrue(file1Found);
            Assert.assertFalse(file2Found);
        }
    }

}
