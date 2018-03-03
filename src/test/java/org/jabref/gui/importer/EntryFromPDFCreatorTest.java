package org.jabref.gui.importer;

import java.io.File;
import java.util.Optional;

import org.jabref.JabRefGUI;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.importer.ImportDataTest;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@GUITest
public class EntryFromPDFCreatorTest {

    private EntryFromPDFCreator entryCreator;

    @BeforeEach
    public void setUp() {
        // Needed to initialize ExternalFileTypes
        entryCreator = new EntryFromPDFCreator(mock(ExternalFileTypes.class, Answers.RETURNS_DEEP_STUBS));

        // Needed for PdfImporter - still not enough
        JabRefGUI.setMainFrame(mock(JabRefFrame.class));
    }

    @Test
    public void testPDFFileFilter() {
        assertTrue(entryCreator.accept(new File("aPDF.pdf")));
        assertTrue(entryCreator.accept(new File("aPDF.PDF")));
        assertFalse(entryCreator.accept(new File("foo.jpg")));
    }

    @Test
    public void testCreationOfEntryNoPDF() {
        Optional<BibEntry> entry = entryCreator.createEntry(ImportDataTest.NOT_EXISTING_PDF, false);
        assertFalse(entry.isPresent());
    }

    @Test
    @Disabled //Can't mock basepanel and maintable
    public void testCreationOfEntryNotInDatabase() {
        Optional<BibEntry> entry = entryCreator.createEntry(ImportDataTest.FILE_NOT_IN_DATABASE, false);
        assertTrue(entry.isPresent());
        assertTrue(entry.get().getField("file").get().endsWith(":PDF"));
        assertEquals(Optional.of(ImportDataTest.FILE_NOT_IN_DATABASE.getName()),
                entry.get().getField("title"));

    }
}
