package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class ExporterTest {

    public Exporter exportFormat;
    public String exportFormatName;
    public BibDatabaseContext databaseContext;
    public Charset charset;
    public List<BibEntry> entries;

    public ExporterTest(Exporter format, String name) {
        exportFormat = format;
        exportFormatName = name;
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Before
    public void setUp() {
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        entries = Collections.emptyList();
    }

    @Parameterized.Parameters(name = "{index}: {1}")
    public static Collection<Object[]> exportFormats() {
        Collection<Object[]> result = new ArrayList<>();

        Map<String, TemplateExporter> customFormats = new HashMap<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences);

        for (Exporter format : exporterFactory.getExporters()) {
            result.add(new Object[]{format, format.getDisplayName()});
        }
        return result;
    }

    @Test
    public void testExportingEmptyDatabaseYieldsEmptyFile() throws Exception {
        Path tmpFile = testFolder.newFile().toPath();
        exportFormat.export(databaseContext, tmpFile, charset, entries);
        assertEquals(Collections.emptyList(), Files.readAllLines(tmpFile));
    }

    @Test(expected = NullPointerException.class)
    public void testExportingNullDatabaseThrowsNPE() throws Exception {
        Path tmpFile = testFolder.newFile().toPath();
        exportFormat.export(null, tmpFile, charset, entries);
    }

    @Test(expected = NullPointerException.class)
    public void testExportingNullEntriesThrowsNPE() throws Exception {
        Path tmpFile = testFolder.newFile().toPath();
        exportFormat.export(databaseContext, tmpFile, charset, null);
    }
}
