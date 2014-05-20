package net.sf.jabref.imports;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefPreferences;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @version 11.11.2008 | 22:16
 */
public class EntryFromPDFCreatorTest {

    private EntryFromPDFCreator entryCreator = new EntryFromPDFCreator();

    @Before
    public void setUp() throws Exception {
        // externalFileTypes are needed for the EntryFromPDFCreator
        JabRefPreferences.getInstance().updateExternalFileTypes();
    }

    @Test
    public void testPDFFileFilter() {
        assertTrue(entryCreator.accept(new File("aPDF.pdf")));
        assertTrue(entryCreator.accept(new File("aPDF.PDF")));
        assertFalse(entryCreator.accept(new File("foo.jpg")));
    }

    @Test @Ignore
    public void testCreationOfEntry() {
        BibtexEntry entry = entryCreator.createEntry(ImportDataTest.NOT_EXISTING_PDF, false);
        assertNull(entry);

        entry = entryCreator.createEntry(ImportDataTest.FILE_NOT_IN_DATABASE, false);
        assertNotNull(entry);
        assertTrue(entry.getField("file").endsWith(":PDF"));
        assertEquals(ImportDataTest.FILE_NOT_IN_DATABASE.getName(), entry.getField("title"));

    }
} 