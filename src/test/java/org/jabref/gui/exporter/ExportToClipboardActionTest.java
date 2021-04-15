package org.jabref.gui.exporter;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Answers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class ExportToClipboardActionTest {

    private ExportToClipboardAction exportToClipboardAction;
    private LibraryTab libraryTab = mock(LibraryTab.class);
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private DialogService dialogService = spy(DialogService.class);
    private ExporterFactory exporterFactory;
    private ClipBoardManager clipBoardManager = mock(ClipBoardManager.class);
    private TaskExecutor taskExecutor = spy(TaskExecutor.class);

    private List<BibEntry> selectedEntries;

    @BeforeEach
    public void setUp() {
        List<TemplateExporter> customFormats = new ArrayList<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences);
        exportToClipboardAction = spy(new ExportToClipboardAction(libraryTab, dialogService, exporterFactory, clipBoardManager, taskExecutor));

        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.AUTHOR, "Souti Chattopadhyay and Nicholas Nelson and Audrey Au and Natalia Morales and Christopher Sanchez and Rahul Pandita and Anita Sarma")
                .withField(StandardField.TITLE, "A tale from the trenches")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1145/3377811.3380330")
                .withField(StandardField.SUBTITLE, "cognitive biases and software development");

        selectedEntries = new ArrayList<>();
        selectedEntries.add(entry);
    }

    @Test
    public void testExecuteIfNoSelectedEntries() {
        when(libraryTab.getSelectedEntries()).thenReturn(Collections.EMPTY_LIST);

        exportToClipboardAction.execute();
        verify(dialogService, times(1)).notify(Localization.lang("This operation requires one or more entries to be selected."));
    }

    @Test
    public void testExecuteOnSuccess() {
        when(libraryTab.getSelectedEntries()).thenReturn(selectedEntries);

        exportToClipboardAction.execute();
        verify(dialogService, times(1)).showChoiceDialogAndWait(
                Localization.lang("Export"), Localization.lang("Select export format"),
                Localization.lang("Export"), any(Exporter.class), anyCollection());
        verify(dialogService, times(1)).notify(Localization.lang("Entries exported to clipboard") + ": " + selectedEntries.size());
    }
}
