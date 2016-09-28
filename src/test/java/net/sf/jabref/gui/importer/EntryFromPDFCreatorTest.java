package net.sf.jabref.gui.importer;

import java.io.File;
import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.importer.ImportDataTest;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class EntryFromPDFCreatorTest {

    private EntryFromPDFCreator entryCreator;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        // Needed to initialize ExternalFileTypes
        entryCreator = new EntryFromPDFCreator();
        // Needed for PdfImporter - still not enough
        JabRefGUI.setMainFrame(mock(JabRefFrame.class));
    }

    @Test
    public void testPDFFileFilter() {
        Assert.assertTrue(entryCreator.accept(new File("aPDF.pdf")));
        Assert.assertTrue(entryCreator.accept(new File("aPDF.PDF")));
        Assert.assertFalse(entryCreator.accept(new File("foo.jpg")));
    }

    @Test
    public void testCreationOfEntryNoPDF() {
        Optional<BibEntry> entry = entryCreator.createEntry(ImportDataTest.NOT_EXISTING_PDF, false);
        Assert.assertFalse(entry.isPresent());
    }

    @Test
    @Ignore //Can't mock basepanel and maintable
    public void testCreationOfEntryNotInDatabase() {
        Optional<BibEntry> entry = entryCreator.createEntry(ImportDataTest.FILE_NOT_IN_DATABASE, false);
        Assert.assertTrue(entry.isPresent());
        Assert.assertTrue(entry.get().getField("file").get().endsWith(":PDF"));
        Assert.assertEquals(Optional.of(ImportDataTest.FILE_NOT_IN_DATABASE.getName()),
                entry.get().getField("title"));

    }
}