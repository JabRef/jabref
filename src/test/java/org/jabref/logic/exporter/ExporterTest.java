package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javafx.collections.FXCollections;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrder;

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
    public List<BibEntry> entries;

    @BeforeEach
    void setUp() {
        databaseContext = new BibDatabaseContext();
        entries = Collections.emptyList();
    }

    private static Stream<Object[]> exportFormats() {
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getExportPreferences().getExportSaveOrder()).thenReturn(SaveOrder.getDefaultSaveOrder());
        when(preferences.getExportPreferences().getCustomExporters()).thenReturn(FXCollections.emptyObservableList());

        ExporterFactory exporterFactory = ExporterFactory.create(
                preferences,
                mock(BibEntryTypesManager.class));

        Collection<Object[]> result = new ArrayList<>();
        for (Exporter format : exporterFactory.getExporters()) {
            result.add(new Object[]{format, format.getName()});
        }
        return result.stream();
    }

    @ParameterizedTest
    @MethodSource("exportFormats")
    void exportingEmptyDatabaseYieldsEmptyFile(Exporter exportFormat, String name, @TempDir Path testFolder) throws Exception {
        Path tmpFile = testFolder.resolve("ARandomlyNamedFile");
        Files.createFile(tmpFile);
        exportFormat.export(databaseContext, tmpFile, entries);
        assertEquals(Collections.emptyList(), Files.readAllLines(tmpFile));
    }

    @ParameterizedTest
    @MethodSource("exportFormats")
    void exportingNullDatabaseThrowsNPE(Exporter exportFormat, String name, @TempDir Path testFolder) {
        assertThrows(NullPointerException.class, () -> {
            Path tmpFile = testFolder.resolve("ARandomlyNamedFile");
            Files.createFile(tmpFile);
            exportFormat.export(null, tmpFile, entries);
        });
    }
}
