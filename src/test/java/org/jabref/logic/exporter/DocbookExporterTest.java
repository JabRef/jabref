package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(TempDirectory.class)
public class DocbookExporterTest {

    private Exporter exportFormat;
    public BibDatabaseContext databaseContext;
    public Charset charset;

    @BeforeEach
    public void setUp() {
        Map<String, TemplateExporter> customFormats = new HashMap<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences);

        exportFormat = exporterFactory.getExporterByName("docbook").get();

        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
    }

    @AfterEach
    public void tearDown() {
        exportFormat = null;
    }

    @Test
    public void testCorruptedTitleBraces(@TempDirectory.TempDir Path testFolder) throws Exception {
        Path tmpFile = testFolder.resolve("testBraces");

        BibEntry entry = new BibEntry();
        entry.setField("title", "Peptidomics of the larval {\\protect{{D}rosophila melanogaster}} central nervous system.");

        List<BibEntry> entries = Arrays.asList(entry);

        exportFormat.export(databaseContext, tmpFile, charset, entries);

        List<String> lines = Files.readAllLines(tmpFile);
        assertEquals(20, lines.size());
        assertEquals("   <citetitle pubwork=\"article\">Peptidomics of the larval Drosophila melanogaster central nervous system.</citetitle>", lines.get(9));
    }

    @Test
    public void testCorruptedTitleUnicode(@TempDirectory.TempDir Path testFolder) throws Exception {
        Path tmpFile = testFolder.resolve("testBraces");

        BibEntry entry = new BibEntry();
        entry.setField("title", "Insect neuropeptide bursicon homodimers induce innate immune and stress genes during molting by activating the {NF}-$\\kappa$B transcription factor Relish.");

        List<BibEntry> entries = Arrays.asList(entry);

        exportFormat.export(databaseContext, tmpFile, charset, entries);

        List<String> lines = Files.readAllLines(tmpFile);
        assertEquals(20, lines.size());
        assertEquals("   <citetitle pubwork=\"article\">Insect neuropeptide bursicon homodimers induce innate immune and stress genes during molting by activating the NF&#45;&#954;B transcription factor Relish.</citetitle>", lines.get(9));
    }

}
