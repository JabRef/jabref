package org.jabref.gui.exporter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.SaveConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.LibraryPreferences;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExportToClipboardActionTest {

    private ExportToClipboardAction exportToClipboardAction;
    private final DialogService dialogService = spy(DialogService.class);
    private final ClipBoardManager clipBoardManager = mock(ClipBoardManager.class);
    private final BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private final PreferencesService preferences = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);
    private final StateManager stateManager = mock(StateManager.class);

    private TaskExecutor taskExecutor;
    private ObservableList<BibEntry> selectedEntries;

    @BeforeEach
    public void setUp() {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.AUTHOR, "Souti Chattopadhyay and Nicholas Nelson and Audrey Au and Natalia Morales and Christopher Sanchez and Rahul Pandita and Anita Sarma")
                .withField(StandardField.TITLE, "A tale from the trenches")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1145/3377811.3380330")
                .withField(StandardField.SUBTITLE, "cognitive biases and software development");

        selectedEntries = FXCollections.observableArrayList(entry);
        when(stateManager.getSelectedEntries()).thenReturn(selectedEntries);

        taskExecutor = new CurrentThreadTaskExecutor();
        when(preferences.getExportPreferences().getCustomExporters()).thenReturn(FXCollections.observableList(List.of()));
        when(preferences.getExportConfiguration()).thenReturn(mock(SaveConfiguration.class));
        when(preferences.getXmpPreferences()).thenReturn(mock(XmpPreferences.class));
        exportToClipboardAction = new ExportToClipboardAction(dialogService, stateManager, clipBoardManager, taskExecutor, preferences);
    }

    @Test
    public void testExecuteIfNoSelectedEntries() {
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.emptyObservableList());

        exportToClipboardAction.execute();
        verify(dialogService, times(1)).notify(Localization.lang("This operation requires one or more entries to be selected."));
    }

    @Test
    public void testExecuteOnSuccess() {
        Exporter selectedExporter = new Exporter("html", "HTML", StandardFileType.HTML) {
            @Override
            public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) {
            }
        };

        LibraryPreferences libraryPreferences = mock(LibraryPreferences.class, Answers.RETURNS_DEEP_STUBS);
        FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getLibraryPreferences()).thenReturn(libraryPreferences);
        when(preferences.getExportPreferences().getLastExportExtension()).thenReturn("HTML");
        when(stateManager.getSelectedEntries()).thenReturn(selectedEntries);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        // noinspection ConstantConditions since databaseContext is mocked
        when(databaseContext.getFileDirectories(preferences.getFilePreferences())).thenReturn(new ArrayList<>(List.of(Path.of("path"))));
        when(databaseContext.getMetaData()).thenReturn(new MetaData());
        when(dialogService.showChoiceDialogAndWait(
                eq(Localization.lang("Export")),
                eq(Localization.lang("Select export format")),
                eq(Localization.lang("Export")),
                any(Exporter.class),
                anyCollection())
        ).thenReturn(Optional.of(selectedExporter));

        exportToClipboardAction.execute();
        verify(dialogService, times(1)).showChoiceDialogAndWait(
                eq(Localization.lang("Export")), eq(Localization.lang("Select export format")),
                eq(Localization.lang("Export")), any(Exporter.class), anyCollection());
        verify(dialogService, times(1)).notify(Localization.lang("Entries exported to clipboard") + ": " + selectedEntries.size());
    }
}
