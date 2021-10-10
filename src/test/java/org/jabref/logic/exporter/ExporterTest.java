package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExporterTest {

    public BibDatabaseContext databaseContext;
    public Charset charset;
    public List<BibEntry> entries;

    @BeforeEach
    public void setUp() {
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        entries = Collections.emptyList();
    }

    private static Stream<Object[]> exportFormats() {
        Collection<Object[]> result = new ArrayList<>();

        List<TemplateExporter> customFormats = new ArrayList<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class);
        SavePreferences savePreferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(savePreferences.getFieldWriterPreferences()).thenReturn(new FieldWriterPreferences());
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        BibEntryTypesManager entryTypesManager = mock(BibEntryTypesManager.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences, BibDatabaseMode.BIBTEX, entryTypesManager);

        for (Exporter format : exporterFactory.getExporters()) {
            result.add(new Object[]{format, format.getName()});
        }
        return result.stream();
    }

    @ParameterizedTest
    @MethodSource("exportFormats")
    public void testExportingEmptyDatabaseYieldsEmptyFile(Exporter exportFormat, String name, @TempDir Path testFolder) throws Exception {
        Path tmpFile = testFolder.resolve("ARandomlyNamedFile");
        Files.createFile(tmpFile);
        exportFormat.export(databaseContext, tmpFile, charset, entries);
        assertEquals(Collections.emptyList(), Files.readAllLines(tmpFile));
    }

    @ParameterizedTest
    @MethodSource("exportFormats")
    public void testExportingNullDatabaseThrowsNPE(Exporter exportFormat, String name, @TempDir Path testFolder) {
        assertThrows(NullPointerException.class, () -> {
            Path tmpFile = testFolder.resolve("ARandomlyNamedFile");
            Files.createFile(tmpFile);
            exportFormat.export(null, tmpFile, charset, entries);
        });
    }

    @ParameterizedTest
    @MethodSource("exportFormats")
    public void testExportingNullEntriesThrowsNPE(Exporter exportFormat, String name, @TempDir Path testFolder) {
        assertThrows(NullPointerException.class, () -> {
            Path tmpFile = testFolder.resolve("ARandomlyNamedFile");
            Files.createFile(tmpFile);
            exportFormat.export(databaseContext, tmpFile, charset, null);
        });
    }
}
