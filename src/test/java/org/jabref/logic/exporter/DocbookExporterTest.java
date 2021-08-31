package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class DocbookExporterTest {

    public BibDatabaseContext databaseContext = new BibDatabaseContext();
    public Charset charset = StandardCharsets.UTF_8;

    private Exporter exportFormat;

    @BeforeEach
    public void setUp() {
        List<TemplateExporter> customFormats = new ArrayList<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        BibEntryTypesManager entryTypesManager = mock(BibEntryTypesManager.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences, BibDatabaseMode.BIBTEX, entryTypesManager);

        exportFormat = exporterFactory.getExporterByName("docbook4").get();
    }

    @Test
    public void testCorruptedTitleBraces(@TempDir Path testFolder) throws Exception {
        Path tmpFile = testFolder.resolve("testBraces");

        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "Peptidomics of the larval {{{D}rosophila melanogaster}} central nervous system.");

        List<BibEntry> entries = Arrays.asList(entry);

        exportFormat.export(databaseContext, tmpFile, charset, entries);

        List<String> lines = Files.readAllLines(tmpFile);
        assertEquals(20, lines.size());
        assertEquals("   <citetitle pubwork=\"article\">Peptidomics of the larval Drosophila melanogaster central nervous system.</citetitle>", lines.get(9));
    }

    @Test
    public void testCorruptedTitleUnicode(@TempDir Path testFolder) throws Exception {
        Path tmpFile = testFolder.resolve("testBraces");

        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "Insect neuropeptide bursicon homodimers induce innate immune and stress genes during molting by activating the {NF}-$\\kappa$B transcription factor Relish.");

        List<BibEntry> entries = Arrays.asList(entry);

        exportFormat.export(databaseContext, tmpFile, charset, entries);

        List<String> lines = Files.readAllLines(tmpFile);
        assertEquals(20, lines.size());
        assertEquals("   <citetitle pubwork=\"article\">Insect neuropeptide bursicon homodimers induce innate immune and stress genes during molting by activating the NF&#45;&#954;B transcription factor Relish.</citetitle>", lines.get(9));
    }

}
