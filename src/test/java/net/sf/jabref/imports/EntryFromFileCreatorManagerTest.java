package net.sf.jabref.imports;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @version 11.11.2008 | 21:51:54
 */
public class EntryFromFileCreatorManagerTest {

    @Test
    public void testGetCreator() throws Exception {
        EntryFromFileCreatorManager manager = new EntryFromFileCreatorManager();
        EntryFromFileCreator creator = manager.getEntryCreator(ImportDataTest.NOT_EXISTING_PDF);
        Assert.assertNull(creator);

        creator = manager.getEntryCreator(ImportDataTest.FILE_IN_DATABASE);
        Assert.assertNotNull(creator);
        Assert.assertTrue(creator.accept(ImportDataTest.FILE_IN_DATABASE));
    }

    @Test
    @Ignore
    public void testAddEntrysFromFiles() throws Exception {
        ParserResult result = BibtexParser.parse(new FileReader(ImportDataTest.UNLINKED_FILES_TEST_BIB));
        BibtexDatabase database = result.getDatabase();

        List<File> files = new ArrayList<File>();

        files.add(ImportDataTest.FILE_NOT_IN_DATABASE);
        files.add(ImportDataTest.NOT_EXISTING_PDF);

        EntryFromFileCreatorManager manager = new EntryFromFileCreatorManager();
        List<String> errors = manager.addEntrysFromFiles(files, database, null, true);

        /**
         * One file doesn't exist, so adding it as an entry should lead to an
         * error message.
         */
        Assert.assertEquals(1, errors.size());

        boolean file1Found = false, file2Found = false;
        for (BibtexEntry entry : database.getEntries()) {
            String filesInfo = entry.getField("file");
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
