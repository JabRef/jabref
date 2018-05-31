package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@FetcherTest //we mark this as fetcher test, because it depends on the avaiability of the FreeCite online library
public class FreeCiteImporterTest {

    private FreeCiteImporter importer;

    @BeforeEach
    public void setUp() {
        importer = new FreeCiteImporter(mock(ImportFormatPreferences.class));
    }

    @Test
    public void freeCiteReturnsSomething() throws IOException {
        String entryText = "Kopp, O.; Martin, D.; Wutke, D. & Leymann, F. The Difference Between Graph-Based and Block-Structured Business Process Modelling Languages Enterprise Modelling and Information Systems, Gesellschaft für Informatik e.V. (GI), 2009, 4, 3-13";
        BufferedReader input = new BufferedReader(new StringReader(entryText));

        List<BibEntry> bibEntries = importer.importDatabase(input).getDatabase().getEntries();
        BibEntry bibEntry = bibEntries.get(0);

        assertEquals(1, bibEntries.size());
        assertEquals(bibEntry.getField("author"), Optional.of("O Kopp and D Martin and D Wutke and F Leymann"));
    }

    @Test
    public void testGetFormatName() {
        assertEquals("text citations", importer.getName());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileType.FREECITE, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("This importer parses text format citations using the online API of FreeCite.",
                importer.getDescription());
    }
}
