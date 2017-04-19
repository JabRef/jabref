package org.jabref.gui.importer;

import java.io.File;
import java.util.Optional;

import org.jabref.JabRefGUI;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.importer.ImportDataTest;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.GUITests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.mockito.Mockito.mock;

@Category(GUITests.class)
public class EntryFromPDFCreatorTest {

    private EntryFromPDFCreator entryCreator;


    @Before
    public void setUp() {
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
