package org.jabref.gui.consistency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

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
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Test
        // [utest->req~ux.consistency.bst-field-columns~2]
    void bstStyleRestrictsShownFieldsUntilSwitchedOff(@TempDir Path tempDir) throws Exception {
        Path bstFile = tempDir.resolve("minimal.bst");
        Files.writeString(bstFile, "ENTRY { author title } {} {}");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();

        loadBstFile(viewModel, bstFile);

        assertEquals("minimal.bst", viewModel.bstStyleNameProperty().get());
        assertTrue(viewModel.restrictToBstFieldsProperty().get());
        assertEquals(List.of(StandardField.TITLE), shownFields(viewModel));

        viewModel.restrictToBstFieldsProperty().set(false);
        assertEquals(viewModel.getReportedFields(), shownFields(viewModel));

        viewModel.restrictToBstFieldsProperty().set(true);
        assertEquals(List.of(StandardField.TITLE), shownFields(viewModel));
    }

    @Test
        // [utest->req~ux.consistency.bst-field-columns~2]
    void bstStyleKeepsAliasedFields(@TempDir Path tempDir) throws Exception {
        Path bstFile = tempDir.resolve("minimal.bst");
        Files.writeString(bstFile, "ENTRY { author journal } {} {}");
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.JOURNALTITLE, "Journal One");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author Two");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntries(List.of(first, second));

        loadBstFile(viewModel, bstFile);

        assertEquals(List.of(StandardField.JOURNAL, StandardField.JOURNALTITLE), shownFields(viewModel));
    }

    @Test
    void anotherBstFileOfTheSameNameReplacesTheFields(@TempDir Path tempDir) throws Exception {
        Path titleStyle = Files.writeString(Files.createDirectory(tempDir.resolve("a")).resolve("style.bst"), "ENTRY { author title } {} {}");
        Path yearStyle = Files.writeString(Files.createDirectory(tempDir.resolve("b")).resolve("style.bst"), "ENTRY { author year } {} {}");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();

        loadBstFile(viewModel, titleStyle);
        loadBstFile(viewModel, yearStyle);

        assertEquals(List.of(StandardField.YEAR), shownFields(viewModel));
    }

    @Test
    void bstFileWithoutEntryFieldsIsRejected(@TempDir Path tempDir) throws Exception {
        Path bstFile = tempDir.resolve("noentry.bst");
        Files.writeString(bstFile, "FUNCTION { nothing } { }");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();

        loadBstFile(viewModel, bstFile);

        verify(dialogService).showErrorDialogAndWait(Localization.lang("You must select a valid .bst style file."));
        assertEquals("", viewModel.bstStyleNameProperty().get());
        assertFalse(viewModel.restrictToBstFieldsProperty().get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"date", "year", "month"})
        // [utest->req~ux.consistency.bst-field-columns~2]
    void dateAndItsPartsAreReadForEachOther(String declaredField, @TempDir Path tempDir) throws Exception {
        Path bstFile = tempDir.resolve("minimal.bst");
        Files.writeString(bstFile, "ENTRY { author " + declaredField + " } {} {}");
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.DATE, "2026-11")
                .withField(StandardField.MONTH, "11");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author Two");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntries(List.of(first, second));

        loadBstFile(viewModel, bstFile);

        assertEquals(List.of(StandardField.DATE, StandardField.MONTH, StandardField.YEAR), shownFields(viewModel));
    }

    @Test
        // [utest->req~ux.consistency.bst-field-columns~2]
    void entryTypesWithoutVisibleColumnAreHidden(@TempDir Path tempDir) throws Exception {
        Path bstFile = tempDir.resolve("minimal.bst");
        Files.writeString(bstFile, "ENTRY { author publisher } {} {}");
        BibEntry article = new BibEntry(StandardEntryType.Article, "article").withField(StandardField.AUTHOR, "Author One");
        BibEntry bookWithPublisher = new BibEntry(StandardEntryType.Book, "first").withField(StandardField.AUTHOR, "Author One").withField(StandardField.PUBLISHER, "Publisher");
        BibEntry book = new BibEntry(StandardEntryType.Book, "second").withField(StandardField.AUTHOR, "Author Two");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntries(List.of(article, bookWithPublisher, book));
        viewModel.selectedEntryTypeProperty().set("Article");

        loadBstFile(viewModel, bstFile);

        assertEquals(List.of("Book"), viewModel.getEntryTypes());
        assertEquals("Book", viewModel.selectedEntryTypeProperty().get());

        viewModel.restrictToBstFieldsProperty().set(false);

        assertEquals(List.of("Article", "Book"), viewModel.getEntryTypes());
    }

    @Test
    void entryTypesRequiringOnlySpecialFieldsAreNotSelectable(@TempDir Path tempDir) throws Exception {
        var type = new UnknownEntryType("specialOnly");
        entryTypesManager.addCustomOrModifiedType(new BibEntryType(type,
                List.of(new BibField(SpecialField.RANKING, FieldPriority.IMPORTANT)),
                List.of(new OrFields(SpecialField.RANKING))), BibDatabaseMode.BIBTEX);
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntries(List.of(new BibEntry(type, "entry")));
        Path bst = Files.writeString(tempDir.resolve("style.bst"), "ENTRY { ranking } {} {}");

        loadBstFile(viewModel, bst);

        assertEquals(List.of(SpecialField.RANKING), viewModel.getReportedFields());
        assertEquals(List.of(), viewModel.getEntryTypes());
        assertNull(viewModel.selectedEntryTypeProperty().get());
    }

    @Test
    void styleCompletionPreservesLaterChoiceToShowAllFields(@TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        Path titleStyle = Files.writeString(tempDir.resolve("title.bst"), "ENTRY { title } {} {}");
        Path yearStyle = Files.writeString(tempDir.resolve("year.bst"), "ENTRY { year } {} {}");
        loadBstFile(viewModel, titleStyle);
        clearInvocations(taskExecutor);
        viewModel.loadBstFile(yearStyle);
        ArgumentCaptor<BackgroundTask<Optional<Predicate<Field>>>> captor = ArgumentCaptor.captor();
        verify(taskExecutor).execute(captor.capture());
        var pending = captor.getValue();
        var result = pending.call();

        viewModel.restrictToBstFieldsProperty().set(false);
        pending.getOnSuccess().accept(result);

        assertFalse(viewModel.restrictToBstFieldsProperty().get());
        assertEquals("year.bst", viewModel.bstStyleNameProperty().get());
        assertEquals(viewModel.getReportedFields(), shownFields(viewModel));
        viewModel.restrictToBstFieldsProperty().set(true);
        assertEquals(List.of(StandardField.YEAR), shownFields(viewModel));
    }

    private void loadBstFile(ConsistencyCheckDialogViewModel viewModel, Path path) throws Exception {
        clearInvocations(taskExecutor);
        viewModel.loadBstFile(path);
        ArgumentCaptor<BackgroundTask<Optional<Predicate<Field>>>> captor = ArgumentCaptor.captor();
        verify(taskExecutor).execute(captor.capture());
        BackgroundTask<Optional<Predicate<Field>>> task = captor.getValue();
        task.getOnSuccess().accept(task.call());
    }

    @Test
    void loadingBstFileRunsInBackgroundAndIgnoresOlderResults(@TempDir Path tempDir) throws Exception {
        Path first = Files.writeString(tempDir.resolve("first.bst"), "ENTRY { title } {} {}");
        Path second = Files.writeString(tempDir.resolve("second.bst"), "ENTRY { year } {} {}");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();

        viewModel.loadBstFile(first);
        viewModel.loadBstFile(second);
        ArgumentCaptor<BackgroundTask<Optional<Predicate<Field>>>> captor = ArgumentCaptor.captor();
        verify(taskExecutor, org.mockito.Mockito.times(2)).execute(captor.capture());
        BackgroundTask<Optional<Predicate<Field>>> older = captor.getAllValues().getFirst();
        BackgroundTask<Optional<Predicate<Field>>> newer = captor.getAllValues().getLast();
        assertEquals("", viewModel.bstStyleNameProperty().get());

        newer.getOnSuccess().accept(newer.call());
        assertThrows(CancellationException.class, older::call);

        assertEquals("second.bst", viewModel.bstStyleNameProperty().get());
        assertEquals(List.of(StandardField.YEAR), shownFields(viewModel));
    }

    @Test
    void failedBstLoadShowsErrorWithoutChangingSelection(@TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        Path missing = tempDir.resolve("missing.bst");
        viewModel.loadBstFile(missing);
        ArgumentCaptor<BackgroundTask<Optional<Predicate<Field>>>> captor = ArgumentCaptor.captor();
        verify(taskExecutor).execute(captor.capture());
        BackgroundTask<Optional<Predicate<Field>>> task = captor.getValue();
        IOException exception = assertThrows(IOException.class, task::call);

        task.getOnException().accept(exception);

        verify(dialogService).showErrorDialogAndWait(Localization.lang("You must select a valid .bst style file."), exception);
        assertEquals("", viewModel.bstStyleNameProperty().get());
    }

    @Test
        // [utest->req~ux.consistency.aux-entry-selection~2]
    void auxSelectionRecomputesColumnsAndRetainsOriginalEntries(@TempDir Path tempDir) throws Exception {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.NOTE, "Note");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second").withField(StandardField.AUTHOR, "Author Two");
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntries(List.of(first, second));
        List<String> fullColumns = viewModel.getColumnNames();
        Path aux = Files.writeString(tempDir.resolve("selected.aux"), "\\citation{first}");
        Path bst = Files.writeString(tempDir.resolve("style.bst"), "ENTRY { title } {} {}");
        loadBstFile(viewModel, bst);
        first.setField(StandardField.TITLE, "Changed after the check");

        BackgroundTask<?> task = scheduleAux(viewModel, aux);
        assertEquals(List.of(first, second), viewModel.getTableData().stream().map(ConsistencyMessage::bibEntry).toList());
        completeTask(task);

        assertEquals("selected.aux", viewModel.auxFileNameProperty().get());
        assertEquals(List.of("Entry Type", "CitationKey", "Journal", "Title", "Year"), viewModel.getColumnNames());
        assertEquals(List.of(first), viewModel.getTableData().stream().map(ConsistencyMessage::bibEntry).toList());
        assertEquals(List.of("Article", "first", "-", "-", "-"), viewModel.getTableData().getFirst().message());
        assertEquals(List.of(StandardField.TITLE), shownFields(viewModel));

        viewModel.restrictToAuxEntriesProperty().set(false);

        assertEquals("selected.aux", viewModel.auxFileNameProperty().get());
        assertEquals(fullColumns, viewModel.getColumnNames());
        assertEquals(List.of(first, second), viewModel.getTableData().stream().map(ConsistencyMessage::bibEntry).toList());
        assertEquals(List.of(StandardField.TITLE), shownFields(viewModel));

        viewModel.restrictToAuxEntriesProperty().set(true);

        assertEquals(List.of(first), viewModel.getTableData().stream().map(ConsistencyMessage::bibEntry).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\\citation{unknown}"})
    void auxWithoutMatchingCitationsShowsEmptyReport(String contents, @TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        Path aux = Files.writeString(tempDir.resolve("empty.aux"), contents);

        completeTask(scheduleAux(viewModel, aux));

        assertEquals(List.of(), viewModel.getTableData());
        assertEquals(List.of(), viewModel.getEntryTypes());
        assertEquals(List.of("Entry Type", "CitationKey"), viewModel.getColumnNames());
        assertTrue(viewModel.hasFindings());
    }

    @Test
    void auxNestedCitationsAndWildcardSelectEntries(@TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        Files.writeString(tempDir.resolve("nested.aux"), "\\citation{second}");
        Path aux = Files.writeString(tempDir.resolve("main.aux"), "\\@input{nested.aux}");

        completeTask(scheduleAux(viewModel, aux));

        assertEquals(List.of("second"), viewModel.getTableData().stream().map(message -> message.message().get(1)).toList());
        Files.writeString(aux, "\\citation{*}");

        completeTask(scheduleAux(viewModel, aux));

        assertEquals(List.of("first", "second"), viewModel.getTableData().stream().map(message -> message.message().get(1)).toList());
    }

    @Test
    void staleAuxResultsAndFailuresDoNotReplaceLatestSelection(@TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        Path first = Files.writeString(tempDir.resolve("first.aux"), "\\citation{first}");
        Path second = Files.writeString(tempDir.resolve("second.aux"), "\\citation{second}");
        BackgroundTask<?> older = scheduleAux(viewModel, first);
        BackgroundTask<?> newer = scheduleAux(viewModel, second);

        completeTask(newer);
        assertThrows(CancellationException.class, older::call);
        older.getOnException().accept(new IOException("Superseded"));

        assertEquals("second.aux", viewModel.auxFileNameProperty().get());
        assertEquals(List.of("second"), viewModel.getTableData().stream().map(message -> message.message().get(1)).toList());
        BackgroundTask<?> pending = scheduleAux(viewModel, first);
        viewModel.restrictToAuxEntriesProperty().set(false);
        assertThrows(CancellationException.class, pending::call);
        assertFalse(viewModel.restrictToAuxEntriesProperty().get());
        assertEquals("second.aux", viewModel.auxFileNameProperty().get());
        assertEquals(List.of("first", "second"), viewModel.getTableData().stream().map(message -> message.message().get(1)).toList());
    }

    @Test
    void failedAuxReadPreservesCurrentReport(@TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        Path aux = Files.writeString(tempDir.resolve("selected.aux"), "\\citation{second}");
        completeTask(scheduleAux(viewModel, aux));
        BackgroundTask<?> task = scheduleAux(viewModel, tempDir.resolve("missing.aux"));
        IOException exception = assertThrows(IOException.class, task::call);

        task.getOnException().accept(exception);

        verify(dialogService).showErrorDialogAndWait(Localization.lang("Could not read the .aux file."), exception);
        assertEquals("selected.aux", viewModel.auxFileNameProperty().get());
        assertEquals(List.of("second"), viewModel.getTableData().stream().map(message -> message.message().get(1)).toList());
    }

    @ParameterizedTest
    @EnumSource(value = StandardFileType.class, names = {"CSV", "TXT"})
    void auxExportsCaptureSelectedReportBeforeReset(StandardFileType fileType, @TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        Path aux = Files.writeString(tempDir.resolve("selected.aux"), "\\citation{second}");
        completeTask(scheduleAux(viewModel, aux));
        clearInvocations(taskExecutor);
        Path output = tempDir.resolve("report");
        BackgroundTask<?> export = scheduleExport(viewModel, fileType, output);
        viewModel.restrictToAuxEntriesProperty().set(false);

        export.call();

        assertFalse(Files.readString(output).contains("first"));
        assertTrue(Files.readString(output).contains("second"));
    }

    @Test
        // [utest->req~ux.consistency.aux-entry-selection~2]
    void singleAuxFileNextToLibraryIsUsedByDefault(@TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        Files.writeString(tempDir.resolve("paper.aux"), "\\citation{second}");
        Files.writeString(tempDir.resolve("other.bib"), "");
        clearInvocations(taskExecutor);

        viewModel.useAuxFileNextTo(tempDir.resolve("library.bib"));

        ArgumentCaptor<BackgroundTask<?>> captor = ArgumentCaptor.captor();
        verify(taskExecutor).execute(captor.capture());
        completeTask(captor.getValue());
        assertEquals("paper.aux", viewModel.auxFileNameProperty().get());
        assertTrue(viewModel.restrictToAuxEntriesProperty().get());
        assertEquals(List.of("second"), viewModel.getTableData().stream().map(message -> message.message().get(1)).toList());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void auxFileIsNotUsedByDefaultUnlessExactlyOne(int auxFileCount, @TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        for (int i = 0; i < auxFileCount; i++) {
            Files.writeString(tempDir.resolve("paper" + i + ".aux"), "\\citation{second}");
        }
        clearInvocations(taskExecutor);

        viewModel.useAuxFileNextTo(tempDir.resolve("library.bib"));

        ArgumentCaptor<BackgroundTask<?>> captor = ArgumentCaptor.captor();
        verify(taskExecutor).execute(captor.capture());
        completeTask(captor.getValue());
        assertEquals("", viewModel.auxFileNameProperty().get());
    }

    @Test
    void automaticAuxReadFailureKeepsFullReportWithoutErrorDialog(@TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        List<ConsistencyMessage> original = List.copyOf(viewModel.getTableData());
        Files.writeString(tempDir.resolve("paper.aux"), "\\@input{missing.aux}");
        viewModel.useAuxFileNextTo(tempDir.resolve("library.bib"));
        ArgumentCaptor<BackgroundTask<?>> captor = ArgumentCaptor.captor();
        verify(taskExecutor).execute(captor.capture());
        BackgroundTask<?> pending = captor.getValue();
        IOException exception = assertThrows(IOException.class, pending::call);

        pending.getOnException().accept(exception);

        verify(dialogService, never()).showErrorDialogAndWait(anyString(), any(Throwable.class));
        assertEquals(original, viewModel.getTableData());
        assertEquals("", viewModel.auxFileNameProperty().get());
        assertFalse(viewModel.restrictToAuxEntriesProperty().get());
    }

    @Test
    void automaticAuxDiscoveryRunsInBackground(@TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        viewModel.useAuxFileNextTo(tempDir.resolve("library.bib"));
        Files.writeString(tempDir.resolve("paper.aux"), "\\citation{second}");
        ArgumentCaptor<BackgroundTask<?>> captor = ArgumentCaptor.captor();
        verify(taskExecutor).execute(captor.capture());

        completeTask(captor.getValue());

        assertEquals("paper.aux", viewModel.auxFileNameProperty().get());
        assertEquals(List.of("second"), viewModel.getTableData().stream().map(message -> message.message().get(1)).toList());
    }

    @Test
    void closingDialogCancelsPendingLoads(@TempDir Path tempDir) throws Exception {
        ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
        Future<?> future = mock(Future.class);
        org.mockito.Mockito.doReturn(future).when(taskExecutor).execute(any(BackgroundTask.class));
        BackgroundTask<?> pending = scheduleAux(viewModel, tempDir.resolve("not-read.aux"));
        viewModel.cancelPendingLoads();
        verify(future).cancel(true);
        assertThrows(CancellationException.class, pending::call);
    }

    private BackgroundTask<?> scheduleAux(ConsistencyCheckDialogViewModel viewModel, Path aux) {
        clearInvocations(taskExecutor);
        viewModel.loadAuxFile(aux);
        ArgumentCaptor<BackgroundTask<?>> captor = ArgumentCaptor.captor();
        verify(taskExecutor).execute(captor.capture());
        return captor.getValue();
    }

    private static <T> void completeTask(BackgroundTask<T> task) throws Exception {
        task.getOnSuccess().accept(task.call());
    }

    private static List<StandardField> shownFields(ConsistencyCheckDialogViewModel viewModel) {
        return viewModel.getReportedFields().stream().filter(viewModel.shownFieldsProperty().get()).map(StandardField.class::cast).toList();
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
        @SuppressWarnings("unchecked")
        void reusedEmptyCellClearsItsSymbolTooltip() {
            ConsistencyCheckDialogViewModel viewModel = createViewModelForEntriesHavingOnlyAuthor();
            interact(() -> {
                Injector.setModelOrService(StateManager.class, mock(StateManager.class));
                Injector.setModelOrService(EntryEditor.class, mock(EntryEditor.class));
                ConsistencyCheckDialog dialog = new ConsistencyCheckDialog(mock(LibraryTab.class), dialogService, viewModel);
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
                assertEquals(ConsistencySymbol.UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY.getDescription(), cell.getTooltip().getText());

                cell.updateIndex(-1);

                assertNull(cell.getTooltip());
                assertNull(cell.getGraphic());
                assertNull(cell.getText());
            });
        }

        @Test
            // [utest->req~ux.consistency.report-background~1]
        void workerPreparesReportBeforeShowingDialog(@TempDir Path tempDir) throws Exception {
            BibEntry entry = new BibEntry(StandardEntryType.Article, "first").withField(StandardField.AUTHOR, "Author One");
            BibDatabaseContext bibContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));
            bibContext.setMode(BibDatabaseMode.BIBTEX);
            bibContext.setDatabasePath(tempDir.resolve("library.bib"));
            Files.writeString(tempDir.resolve("paper.aux"), "\\citation{unknown}");
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

            verify(uiTaskExecutor, never()).execute(any(BackgroundTask.class));

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
        BibliographyConsistencyCheck.Snapshot snapshot = BibliographyConsistencyCheck.snapshot(entries, bibContext.getMode(), entryTypesManager, () -> false);
        return new ConsistencyCheckDialogViewModel(dialogService, preferences, snapshot, taskExecutor);
    }
}
