package org.jabref.gui.consistency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.concurrent.Task;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.ConsistencyMessage;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ResourceLock("Localization.lang")
class ConsistencyCheckDialogViewModelTest {

    private final BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();
    private final DialogService dialogService = mock(DialogService.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);
    private final TaskExecutor taskExecutor = mock(TaskExecutor.class);

    @Test
    void columnsContainMissingRequiredFields() {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();

        assertEquals(List.of("Entry Type", "CitationKey", "Journal", "Title", "Year"), viewModel.getColumnNames());
    }

    @Test
    void rowsShowMissingRequiredFieldsAsAbsent() {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();

        assertEquals(List.of(
                List.of("Article", "first", "-", "-", "-"),
                List.of("Article", "second", "-", "-", "-")
        ), viewModel.getTableData().stream().map(ConsistencyMessage::message).toList());
    }

    @ParameterizedTest
    @EnumSource(value = StandardFileType.class, names = {"CSV", "TXT"})
        // [utest->req~ux.consistency.export-background~1]
    void exportingDoesNotWriteUntilBackgroundTaskRuns(StandardFileType fileType, @TempDir Path tempDir) {
        Path exportPath = tempDir.resolve("result");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();

        scheduleExport(viewModel, fileType, exportPath);

        assertFalse(Files.exists(exportPath));
    }

    @Test
        // [utest->req~ux.consistency.entry-navigation~1]
    void tableActionsRetainOriginalEntries() {
        BibEntry entry = new BibEntry(StandardEntryType.Article, "first").withField(StandardField.AUTHOR, "Author One");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntries(List.of(entry));

        assertSame(entry, viewModel.getTableData().getFirst().bibEntry());
    }

    @Test
        // [utest->req~ux.consistency.export-snapshot~1]
    void csvExportUsesSnapshotAfterEntryChanges(@TempDir Path tempDir) throws Exception {
        Path exportPath = tempDir.resolve("result.csv");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        BackgroundTask<?> task = scheduleExport(viewModel, StandardFileType.CSV, exportPath);
        viewModel.getTableData().getFirst().bibEntry().setField(StandardField.TITLE, "Added after the check");

        task.call();

        assertEquals("""
                entry type,citation key,Journal,Title,Year
                Article,first,-,-,-
                Article,second,-,-,-
                """, Files.readString(exportPath).replace("\r\n", "\n"));
    }

    @Test
        // [utest->req~ux.consistency.export-snapshot~1]
    void txtExportUsesSnapshotAfterEntryChanges(@TempDir Path tempDir) throws Exception {
        Path exportPath = tempDir.resolve("result.txt");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        BackgroundTask<?> task = scheduleExport(viewModel, StandardFileType.TXT, exportPath);
        viewModel.getTableData().getFirst().bibEntry().setField(StandardField.TITLE, "Added after the check");

        task.call();

        assertEquals("""
                | entry type | citation key | Journal | Title | Year |
                | ---------- | ------------ | ------- | ----- | ---- |
                | Article    | first        | -       | -     | -    |
                | Article    | second       | -       | -     | -    |
                """, Files.readString(exportPath).replace("\r\n", "\n"));
    }

    @Test
    void failedExportShowsErrorDialog(@TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        BackgroundTask<?> task = scheduleExport(viewModel, StandardFileType.CSV, tempDir.resolve("missing/result.csv"));
        IOException exception = assertThrows(IOException.class, task::call);

        task.getOnException().accept(exception);

        verify(dialogService).showErrorDialogAndWait(Localization.lang("Failed to export file."));
    }

    private BackgroundTask<?> scheduleExport(ConsistencyCheckDialogViewModel viewModel, StandardFileType fileType, Path exportPath) {
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getWorkingDirectory()).thenReturn(exportPath.getParent());
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.of(exportPath));
        switch (fileType) {
            case CSV ->
                    viewModel.startExportAsCsv();
            case TXT ->
                    viewModel.startExportAsTxt();
            default ->
                    throw new AssertionError("Unexpected file type: " + fileType);
        }
        ArgumentCaptor<BackgroundTask<?>> taskCaptor = ArgumentCaptor.captor();
        verify(taskExecutor).execute(taskCaptor.capture());
        return taskCaptor.getValue();
    }

    @Nested
    @NullMarked
    class DialogTest extends JavaFxTest {

        @AfterEach
        void clearInjector() {
            Injector.forgetAll();
        }

        @Test
            // [utest->req~ux.consistency.missing-field-columns~1]
        void dialogKeepsUniformlyMissingRequiredColumns() {
            ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();

            interact(() -> {
                Injector.setModelOrService(StateManager.class, mock(StateManager.class));
                Injector.setModelOrService(EntryEditor.class, mock(EntryEditor.class));
                ConsistencyCheckDialog dialog = new ConsistencyCheckDialog(mock(LibraryTab.class), dialogService, viewModel);
                TableView<?> table = JavaFxExtension.lookup(dialog.getDialogPane(), "#tableView", TableView.class);

                assertEquals(List.of("Entry Type", "CitationKey", "Journal", "Title", "Year"),
                        table.getColumns().stream().map(TableColumn::getText).toList());
            });
        }

        @Test
        @SuppressWarnings("unchecked")
        void clickingMissingFieldSelectsOriginalEntryBeforeRequestingFocus() {
            ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
            LibraryTab libraryTab = mock(LibraryTab.class);
            EntryEditor entryEditor = mock(EntryEditor.class);
            JabRefGuiStateManager stateManager = new JabRefGuiStateManager();

            interact(() -> {
                Injector.setModelOrService(StateManager.class, stateManager);
                Injector.setModelOrService(EntryEditor.class, entryEditor);
                ConsistencyCheckDialog dialog = new ConsistencyCheckDialog(libraryTab, dialogService, viewModel);
                TableView<ConsistencyMessage> table = JavaFxExtension.lookup(dialog.getDialogPane(), "#tableView", TableView.class);
                TableColumn<ConsistencyMessage, String> column = (TableColumn<ConsistencyMessage, String>) table.getColumns().get(3);
                TableRow<ConsistencyMessage> row = new TableRow<>();
                row.updateTableView(table);
                row.updateIndex(0);
                TableCell<ConsistencyMessage, String> cell = column.getCellFactory().call(column);
                cell.updateTableView(table);
                cell.updateTableRow(row);
                cell.updateTableColumn(column);
                cell.updateIndex(0);

                cell.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, true, new PickResult(cell, 0, 0)));

                var navigation = org.mockito.Mockito.inOrder(libraryTab, entryEditor);
                navigation.verify(libraryTab).clearAndSelect(viewModel.getTableData().getFirst().bibEntry());
                navigation.verify(entryEditor).setFocusToField(StandardField.TITLE);
            });
        }

        @Test
            // [utest->req~ux.consistency.report-background~1]
        void workerPreparesReportBeforeShowingDialog() throws Exception {
            BibEntry entry = new BibEntry(StandardEntryType.Article, "first").withField(StandardField.AUTHOR, "Author One");
            BibDatabaseContext bibContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));
            bibContext.setMode(BibDatabaseMode.BIBTEX);
            JabRefGuiStateManager stateManager = new JabRefGuiStateManager();
            stateManager.setActiveDatabase(bibContext);
            LibraryTab libraryTab = mock(LibraryTab.class);
            UiTaskExecutor uiTaskExecutor = mock(UiTaskExecutor.class);
            ConsistencyCheckAction action = new ConsistencyCheckAction(() -> libraryTab, dialogService, stateManager, preferences, entryTypesManager, uiTaskExecutor);
            interact(action::execute);
            ArgumentCaptor<Task<ConsistencyCheckDialogViewModel>> taskCaptor = ArgumentCaptor.captor();
            verify(uiTaskExecutor).execute(taskCaptor.capture());
            Task<ConsistencyCheckDialogViewModel> task = taskCaptor.getValue();
            interact(() -> task.setOnSucceeded(_ -> {
            }));

            ConsistencyCheckDialogViewModel viewModel;
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                executor.submit(task);
                viewModel = task.get(10, TimeUnit.SECONDS);
            }

            assertEquals(List.of(List.of("Article", "first", "-", "-", "-")),
                    viewModel.getTableData().stream().map(ConsistencyMessage::message).toList());
        }
    }

    /// Both entries have the same fields. Thus, they are reported only because of the missing required fields.
    private ConsistencyCheckDialogViewModel createViewModelForEntriesHavingOnlyAuthor() {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author Two");
        return createViewModelForEntries(List.of(first, second));
    }

    private ConsistencyCheckDialogViewModel createViewModelForEntries(List<BibEntry> entries) {
        BibDatabaseContext bibContext = new BibDatabaseContext(new BibDatabase(entries));
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });
        return new ConsistencyCheckDialogViewModel(dialogService, preferences, entryTypesManager, bibContext.getMode(), result, taskExecutor);
    }
}
