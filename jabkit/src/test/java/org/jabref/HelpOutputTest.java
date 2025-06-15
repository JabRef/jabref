package org.jabref;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.util.Pair;

import org.jabref.cli.ArgumentProcessor;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
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
    }

    @Test
    public void testExportFormatFooterShownOnlyForCommandsWithOutputOption() {
        List<Pair<String, String>> inputFormats = ArgumentProcessor.getAvailableImportFormats(preferences);
        List<Pair<String, String>> outputFormats = ArgumentProcessor.getAvailableExportFormats(preferences);
        Set<SearchBasedFetcher> fetchers = WebFetchers.getSearchBasedFetchers(
                preferences.getImportFormatPreferences(),
                preferences.getImporterPreferences()
        );

        JabKit.applyUsageFooters(cmd, inputFormats, outputFormats, fetchers);

        cmd.getSubcommands().forEach((name, subCmd) -> {
            CommandLine.Model.CommandSpec spec = subCmd.getCommandSpec();
            String[] footer = subCmd.getCommandSpec().usageMessage().footer();
            String target = "Available export formats";

            boolean hasOutputOption = spec.options().stream()
                                          .anyMatch(opt -> Arrays.asList(opt.names()).contains("--output"));
            boolean containsExportFormats = Arrays.stream(footer)
                    .anyMatch(line -> line.contains(target));

            if ("fetch".equals(name)) {
                // special case: expect footer NOT present
                assertEquals(false, containsExportFormats,
                        "Did not expect 'Available export formats' in footer for special case: " + name);
            } else if (hasOutputOption) {
                assertEquals(true, containsExportFormats,
                        "Expected 'Available export formats' in footer for: " + name);
            } else {
                assertEquals(false, containsExportFormats,
                        "Did not expect 'Available export formats' in footer for: " + name);
            }
        });
    }
}
