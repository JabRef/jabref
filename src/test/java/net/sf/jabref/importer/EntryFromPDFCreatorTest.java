package net.sf.jabref.importer;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

/**
 * @version 11.11.2008 | 22:16
 */
public class EntryFromPDFCreatorTest {

    private final EntryFromPDFCreator entryCreator = new EntryFromPDFCreator();


    @Before
    public void setUp() throws Exception {
        // externalFileTypes are needed for the EntryFromPDFCreator
        JabRefPreferences.getInstance().updateExternalFileTypes();
    }

    @Test
    public void testPDFFileFilter() {
        Assert.assertTrue(entryCreator.accept(new File("aPDF.pdf")));
        Assert.assertTrue(entryCreator.accept(new File("aPDF.PDF")));
        Assert.assertFalse(entryCreator.accept(new File("foo.jpg")));
    }

    @Test
    @Ignore
    public void testCreationOfEntry() {
        BibEntry entry = entryCreator.createEntry(ImportDataTest.NOT_EXISTING_PDF, false);
        Assert.assertNull(entry);

        entry = entryCreator.createEntry(ImportDataTest.FILE_NOT_IN_DATABASE, false);
        Assert.assertNotNull(entry);
        Assert.assertTrue(entry.getField("file").endsWith(":PDF"));
        Assert.assertEquals(ImportDataTest.FILE_NOT_IN_DATABASE.getName(), entry.getField("title"));

    }
}