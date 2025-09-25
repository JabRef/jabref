package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import javafx.collections.FXCollections;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExporterTest {

    private static Stream<Object[]> exportFormats() {
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getExportPreferences().getExportSaveOrder()).thenReturn(SaveOrder.getDefaultSaveOrder());
        when(preferences.getExportPreferences().getCustomExporters()).thenReturn(FXCollections.emptyObservableList());
        when(preferences.getCustomEntryTypesRepository()).thenReturn(mock(BibEntryTypesManager.class));

        ExporterFactory exporterFactory = ExporterFactory.create(preferences);

        Collection<Object[]> result = new ArrayList<>();
        for (Exporter format : exporterFactory.getExporters()) {
            result.add(new Object[] {format, format.getName()});
        }
        return result.stream();
    }

    @ParameterizedTest
    @MethodSource("exportFormats")
    void exportingEmptyDatabaseYieldsEmptyFile(Exporter exportFormat, String name, @TempDir Path testFolder) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        Path tmpFile = testFolder.resolve("ARandomlyNamedFile");
        Files.createFile(tmpFile);
        exportFormat.export(new BibDatabaseContext(), tmpFile, List.of());
        assertEquals(List.of(), Files.readAllLines(tmpFile));
    }
}
