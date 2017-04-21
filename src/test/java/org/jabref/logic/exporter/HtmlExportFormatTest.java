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
import static org.mockito.Mockito.mock;

public class HtmlExportFormatTest {
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

        exportFormat = ExportFormats.getExportFormat("html");

        databaseContext = new BibDatabaseContext();
        charset = Charsets.UTF_8;
        BibEntry entry = new BibEntry();
        entry.setField("title", "my paper title");
        entry.setField("author", "Stefan Kolb");
        entry.setCiteKey("mykey");
        entries = Arrays.asList(entry);
    }

    @After
    public void tearDown() {
        exportFormat = null;
    }

    @Test
    public void emitWellFormedHtml() throws Exception {
        File tmpFile = testFolder.newFile();
        String filename = tmpFile.getCanonicalPath();
        exportFormat.performExport(databaseContext, filename, charset, entries);
        List<String> lines = Files.readAllLines(tmpFile.toPath());
        assertEquals("</html>", lines.get(lines.size() - 1));
    }
}
