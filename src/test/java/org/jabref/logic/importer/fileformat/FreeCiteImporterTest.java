package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.FileExtensions;
import org.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class FreeCiteImporterTest {

    private FreeCiteImporter importer;


    @Before
    public void setUp() {
        importer = new FreeCiteImporter(mock(ImportFormatPreferences.class));
    }

    @Test
    public void freeCiteReturnsSomething() throws IOException {
        String entryText = "Kopp, O.; Martin, D.; Wutke, D. & Leymann, F. The Difference Between Graph-Based and Block-Structured Business Process Modelling Languages Enterprise Modelling and Information Systems, Gesellschaft für Informatik e.V. (GI), 2009, 4, 3-13";
        BufferedReader input = new BufferedReader(new StringReader(entryText));
        List<BibEntry> bibEntries = importer.importDatabase(input).getDatabase().getEntries();
        assertEquals(1, bibEntries.size());
        BibEntry bibEntry = bibEntries.get(0);
        assertEquals(bibEntry.getField("author"), Optional.of("O Kopp and D Martin and D Wutke and F Leymann"));
    }

    @Test
    public void testGetFormatName() {
        assertEquals("text citations", importer.getName());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileExtensions.FREECITE, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        assertEquals("This importer parses text format citations using the online API of FreeCite.",
                importer.getDescription());
    }
}
