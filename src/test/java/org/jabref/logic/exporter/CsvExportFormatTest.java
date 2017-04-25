package org.jabref.logic.exporter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.google.common.base.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class CsvExportFormatTest {
    private IExportFormat exportFormat;
    public BibDatabaseContext databaseContext;
    public Charset charset;
    public List<BibEntry> entries;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        Map<String, ExportFormat> customFormats = new HashMap<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        ExportFormats.initAllExports(customFormats, layoutPreferences, savePreferences);

        exportFormat = ExportFormats.getExportFormat("oocsv");

        databaseContext = new BibDatabaseContext();
        charset = Charsets.UTF_8;

        BibEntry entry = new BibEntry();
        entry.setField("title", "title1");
        entry.setField("author", "Someone, Van Something");
        entry.setCiteKey("mykey1");

        BibEntry entry2 = new BibEntry();
        entry2.setField("title", "title2");
        entry2.setField("author", "von Neumann, John and Smith, John and Black Brown, Peter");
        entry2.setCiteKey("mykey2");

        BibEntry entry3 = new BibEntry();
        entry3.setField("title", "title3");
        entry3.setField("editor", "Smith, John and Black Brown, Peter");
        entry3.setCiteKey("mykey3");

        entries = Arrays.asList(entry, entry2, entry3);
    }

    @After
    public void tearDown() {
        exportFormat = null;
    }

    @Test
    public void testAuthorsAreSeparatedBySemicolon() throws Exception {
        File tmpFile = testFolder.newFile();
        String filename = tmpFile.getCanonicalPath();

        exportFormat.performExport(databaseContext, filename, charset, entries);

        List<String> lines = Files.readAllLines(tmpFile.toPath());
        assertEquals(4, lines.size());
        System.out.println(lines.get(3));
        assertTrue(lines.get(1).matches("^.*,\"Someone, Van Something\",.*$"));
        assertTrue(lines.get(2).matches("^.*,\"von Neumann, John; Smith, John; Black Brown, Peter\",.*$"));
        assertTrue(lines.get(3).matches("^.*,\"Smith, John; Black Brown, Peter\",.*$"));
    }
}
