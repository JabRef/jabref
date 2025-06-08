package org.jabref;

import java.util.Arrays;

import javafx.collections.FXCollections;

import org.jabref.cli.ArgumentProcessor;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HelpOutputTest {

    private CliPreferences preferences;
    private BibEntryTypesManager entryTypesManager;
    private CommandLine cmd;

    @BeforeEach
    public void setUp() {
        preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        entryTypesManager = mock(BibEntryTypesManager.class);

        ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ExportPreferences exportPreferences = mock(ExportPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

        when(preferences.getImporterPreferences()).thenReturn(importerPreferences);
        when(preferences.getExportPreferences()).thenReturn(exportPreferences);
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);

        when(exportPreferences.getCustomExporters()).thenReturn(FXCollections.emptyObservableList());
        when(importerPreferences.getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());

        ArgumentProcessor argumentProcessor = new ArgumentProcessor(preferences, entryTypesManager);
        cmd = new CommandLine(argumentProcessor);

        JabKit.applyUsageFooters(
                cmd,
                ArgumentProcessor.getAvailableImportFormats(preferences),
                ArgumentProcessor.getAvailableExportFormats(preferences),
                WebFetchers.getSearchBasedFetchers(preferences.getImportFormatPreferences(), preferences.getImporterPreferences())
        );
    }

    @Test
    public void testExportFormatFooterShownOnlyForCommandsWithOutputOption() {
        cmd.getSubcommands().forEach((name, subCmd) -> {
            CommandLine.Model.CommandSpec spec = subCmd.getCommandSpec();
            String helpMessage = subCmd.getUsageMessage();

            boolean hasOutputOption = spec.options().stream()
                                          .anyMatch(opt -> Arrays.asList(opt.names()).contains("--output"));

            if ("fetch".equals(name)) {
                // special case: expect footer NOT present
                assertEquals(false, helpMessage.contains("Available export formats"),
                        "Did not expect 'Available export formats' in help for special case: " + name);
            } else if (hasOutputOption) {
                assertEquals(true, helpMessage.contains("Available export formats"),
                        "Expected 'Available export formats' in help for: " + name);
            } else {
                assertEquals(false, helpMessage.contains("Available export formats"),
                        "Did not expect 'Available export formats' in help for: " + name);
            }
        });
    }
}
