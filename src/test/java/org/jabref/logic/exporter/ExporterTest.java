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

import org.jabref.logic.bibtex.FieldPreferences;
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

        ExporterFactory exporterFactory = ExporterFactory.create(
                new ArrayList<>(),
                mock(LayoutFormatterPreferences.class),
                mock(SaveConfiguration.class),
                mock(XmpPreferences.class),
                mock(FieldPreferences.class, Answers.RETURNS_DEEP_STUBS),
                BibDatabaseMode.BIBTEX,
                mock(BibEntryTypesManager.class));

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
        exportFormat.export(databaseContext, tmpFile, entries);
        assertEquals(Collections.emptyList(), Files.readAllLines(tmpFile));
    }

    @ParameterizedTest
    @MethodSource("exportFormats")
    public void testExportingNullDatabaseThrowsNPE(Exporter exportFormat, String name, @TempDir Path testFolder) {
        assertThrows(NullPointerException.class, () -> {
            Path tmpFile = testFolder.resolve("ARandomlyNamedFile");
            Files.createFile(tmpFile);
            exportFormat.export(null, tmpFile, entries);
        });
    }
}
