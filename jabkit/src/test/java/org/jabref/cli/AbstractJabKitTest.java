package org.jabref.cli;

import java.util.EnumSet;

import javafx.collections.FXCollections;

import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import picocli.CommandLine;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractJabKitTest {
    protected final CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
    protected final BibEntryTypesManager entryTypesManager = mock(BibEntryTypesManager.class);
    protected final ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    protected final ExportPreferences exportPreferences = mock(ExportPreferences.class, Answers.RETURNS_DEEP_STUBS);
    protected final ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

    protected CommandLine commandLine;

    @BeforeEach()
    void setup() {
        when(importerPreferences.getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());
        when(exportPreferences.getCustomExporters()).thenReturn(FXCollections.emptyObservableList());

        when(preferences.getExportPreferences()).thenReturn(exportPreferences);
        when(preferences.getImporterPreferences()).thenReturn(importerPreferences);
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);
        when(preferences.getSearchPreferences()).thenReturn(new SearchPreferences(
                SearchDisplayMode.FILTER,
                EnumSet.noneOf(SearchFlags.class),
                false,
                false,
                0,
                0,
                0));

        ArgumentProcessor argumentProcessor = new ArgumentProcessor(preferences, entryTypesManager);
        commandLine = new CommandLine(argumentProcessor);
    }
}
