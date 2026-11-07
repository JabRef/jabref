package org.jabref.gui.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Labeled;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.util.DialogButtonAssertions;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntryPreferences;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AllowedToUseClassGetResource("JavaFX internally handles the passed URLs properly.")
class BackupResolverDialogTest extends JavaFxTest {

    private static final String FONT_SIZE_CLASS = "font-size-12";
    private static final String CALCULATING = "Calculating...";
    private static final String UNKNOWN = "–";

    private static class DeferringTaskExecutor extends CurrentThreadTaskExecutor {
        private final List<BackgroundTask<?>> deferredTasks = new ArrayList<>();

        @Override
        public <V> Future<V> execute(BackgroundTask<V> task) {
            deferredTasks.add(task);
            return CompletableFuture.completedFuture(null);
        }

        void runNextDeferredTask() {
            super.execute(deferredTasks.removeFirst());
        }

        int deferredTaskCount() {
            return deferredTasks.size();
        }

        boolean firstDeferredTaskIsCancelled() {
            return deferredTasks.getFirst().isCancelled();
        }
    }

    private BackupResolverDialog backupResolverDialog;

    private ListChangeListener<Window> raisedFontSizeListener;

    @BeforeEach
    void initLocalization() {
        Localization.setLanguage(Language.ENGLISH);
    }

    @Override
    public void start(Stage stage) {
        Injector.setModelOrService(KeyBindingRepository.class, mock(KeyBindingRepository.class));

        Scene mainWindowScene = new Scene(new StackPane(), 1024, 768);
        mainWindowScene.getStylesheets().addAll(
                stylesheet("/org/jabref/gui/theme/themes.jabref.org/jabref-theme.css"),
                stylesheet("/org/jabref/gui/theme/internal/jabref-base.css"));
        stage.setScene(mainWindowScene);

        raisedFontSizeListener = change -> {
            while (change.next()) {
                change.getAddedSubList().stream()
                      .map(Window::getScene)
                      .filter(Objects::nonNull)
                      .forEach(scene -> scene.getRoot().getStyleClass().add(FONT_SIZE_CLASS));
            }
        };
        Window.getWindows().addListener(raisedFontSizeListener);

        backupResolverDialog = new BackupResolverDialog(
                Path.of("library.bib"),
                Path.of("backups"),
                mock(ExternalApplicationsPreferences.class),
                mock(ImportFormatPreferences.class),
                new CurrentThreadTaskExecutor());
        backupResolverDialog.initOwner(stage);
        interact(backupResolverDialog::show);
    }

    @AfterEach
    void removeRaisedFontSizeListener() {
        if (raisedFontSizeListener != null) {
            interact(() -> Window.getWindows().removeListener(raisedFontSizeListener));
            raisedFontSizeListener = null;
        }
    }

    /// The dialog has three buttons with long captions, so they only fit when the window grows to the
    /// size the raised font needs.
    @Test
    void buttonCaptionsAreNotTruncatedAtARaisedFontSize() {
        awaitEvents();

        interact(() -> DialogButtonAssertions.assertCaptionsAreNotTruncated(backupResolverDialog.getDialogPane()));
    }

    @Test
    void windowIsAsWideAsItsContentNeeds() {
        awaitEvents();

        interact(() -> {
            DialogPane pane = backupResolverDialog.getDialogPane();
            double windowWidth = pane.getScene().getWindow().getWidth();
            assertTrue(windowWidth >= pane.prefWidth(-1),
                    "Window is %.1fpx wide, but its content needs %.1fpx".formatted(windowWidth, pane.prefWidth(-1)));
        });
    }

    // [utest->req~jabgui.backup.restore-dialog-non-blocking~1]
    // [utest->req~jabgui.backup.restore-dialog-comparison~1]
    @Test
    void showsCalculatingUntilIdenticalFileComparisonCompletes(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "@Article{same, title = {Same}}");
        Path backup = createBackup(original, tempDir.resolve("backups"), "@Article{same, title = {Same}}");
        DeferringTaskExecutor taskExecutor = new DeferringTaskExecutor();

        BackupResolverDialog dialog = createDialog(original, backup.getParent(), taskExecutor);

        assertEquals(1, taskExecutor.deferredTaskCount());
        assertEquals(List.of(CALCULATING, CALCULATING, CALCULATING), List.of(
                textOf(dialog, "currentEntries"),
                textOf(dialog, "backupEntries"),
                textOf(dialog, "backupChanges")));
        assertEquals(List.of(false, false, false), buttonDisabledStates(dialog));

        interact(taskExecutor::runNextDeferredTask);

        assertEquals(List.of("1", "1", "No entry changes"), List.of(
                textOf(dialog, "currentEntries"),
                textOf(dialog, "backupEntries"),
                textOf(dialog, "backupChanges")));
    }

    // [utest->req~jabgui.backup.restore-dialog-non-blocking~1]
    @Test
    void closingDialogCancelsDeferredComparison(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "@Article{current}");
        Path backup = createBackup(original, tempDir.resolve("backups"), "@Article{backup}");
        DeferringTaskExecutor taskExecutor = new DeferringTaskExecutor();
        BackupResolverDialog dialog = createDialog(original, backup.getParent(), taskExecutor);

        interact(dialog::show);
        interact(dialog::close);

        assertTrue(taskExecutor.firstDeferredTaskIsCancelled());
    }

    // [utest->req~jabgui.backup.restore-dialog-comparison~1]
    @Test
    void showsFileSummariesAndEntryChanges(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), """
                @Article{unchanged,
                  title = {Unchanged},
                }
                @Article{removed,
                  title = {Removed},
                }
                @Article{modified,
                  title = {Before},
                }
                """);
        Path backup = createBackup(original, tempDir.resolve("backups"), """
                @Article{unchanged,
                  title = {Unchanged},
                }
                @Article{added,
                  title = {Added},
                }
                @Article{modified,
                  title = {After},
                }
                """);

        BackupResolverDialog dialog = createDialog(original, backup.getParent(), new CurrentThreadTaskExecutor());

        assertEquals(original.getFileName().toString(), textOf(dialog, "currentName"));
        Hyperlink backupLink = assertInstanceOf(Hyperlink.class, dialog.getDialogPane().lookup("#backupName"));
        assertEquals(backup.getFileName().toString(), backupLink.getText());
        assertEquals(FileUtil.getFileSize(original).orElseThrow(), textOf(dialog, "currentSize"));
        assertEquals(FileUtil.getFileSize(backup).orElseThrow(), textOf(dialog, "backupSize"));
        assertEquals(formatModified(original), textOf(dialog, "currentModified"));
        assertEquals(formatModified(backup), textOf(dialog, "backupModified"));
        assertEquals(List.of("3", "3", "removed: 1, added: 1, modified: 1"), List.of(
                textOf(dialog, "currentEntries"),
                textOf(dialog, "backupEntries"),
                textOf(dialog, "backupChanges")));
    }

    @Test
    void missingBackupShowsUnavailableValues(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "@Article{current}");

        BackupResolverDialog dialog = createDialog(
                original,
                tempDir.resolve("missing-backups"),
                new CurrentThreadTaskExecutor());

        assertEquals("File not found", textOf(dialog, "backupName"));
        assertEquals(List.of(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN), List.of(
                textOf(dialog, "backupSize"),
                textOf(dialog, "backupModified"),
                textOf(dialog, "currentEntries"),
                textOf(dialog, "backupEntries"),
                textOf(dialog, "backupChanges")));
    }

    // [utest->req~jabgui.backup.restore-dialog-comparison~1]
    @Test
    void emptyBackupShowsRemovedEntries(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "@Article{current}");
        Path backup = createBackup(original, tempDir.resolve("backups"), "");

        BackupResolverDialog dialog = createDialog(original, backup.getParent(), new CurrentThreadTaskExecutor());

        assertEquals(List.of("1", "0", "removed: 1, added: 0, modified: 0"), List.of(
                textOf(dialog, "currentEntries"),
                textOf(dialog, "backupEntries"),
                textOf(dialog, "backupChanges")));
    }

    // [utest->req~jabgui.backup.restore-dialog-comparison~1]
    @Test
    void unparseableOriginalShowsUnavailableComparison(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), """
                <<<<<<< HEAD
                @Article{current}
                =======
                @Article{other}
                >>>>>>> other
                """);
        Path backup = createBackup(original, tempDir.resolve("backups"), "@Article{backup}");

        BackupResolverDialog dialog = createDialog(original, backup.getParent(), new CurrentThreadTaskExecutor());

        assertEquals(List.of(UNKNOWN, "1", UNKNOWN), List.of(
                textOf(dialog, "currentEntries"),
                textOf(dialog, "backupEntries"),
                textOf(dialog, "backupChanges")));
    }

    // [utest->req~jabgui.backup.restore-dialog-non-blocking~1]
    @Test
    void largeLibraryComparisonIsDeferred(@TempDir Path tempDir) throws IOException {
        String largeLibrary = String.join("\n", IntStream.range(0, 5_000)
                                                         .mapToObj(index -> "@Article{entry" + index + "}")
                                                         .toList());
        Path original = Files.writeString(tempDir.resolve("library.bib"), largeLibrary);
        Path backup = createBackup(original, tempDir.resolve("backups"), largeLibrary);
        DeferringTaskExecutor taskExecutor = new DeferringTaskExecutor();

        BackupResolverDialog dialog = createDialog(original, backup.getParent(), taskExecutor);

        assertEquals(1, taskExecutor.deferredTaskCount());
        assertEquals(List.of(CALCULATING, CALCULATING, CALCULATING), List.of(
                textOf(dialog, "currentEntries"),
                textOf(dialog, "backupEntries"),
                textOf(dialog, "backupChanges")));
    }

    // [utest->req~jabgui.backup.restore-dialog-non-blocking~1]
    @Test
    void showsUnknownValuesWhenDeferredComparisonFails(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "@Article{current}");
        Path backup = createBackup(original, tempDir.resolve("backups"), "@Article{backup}");
        DeferringTaskExecutor taskExecutor = new DeferringTaskExecutor();
        BackupResolverDialog dialog = createDialog(original, backup.getParent(), taskExecutor);
        Files.delete(backup);

        interact(taskExecutor::runNextDeferredTask);

        assertEquals(List.of(UNKNOWN, UNKNOWN, UNKNOWN), List.of(
                textOf(dialog, "currentEntries"),
                textOf(dialog, "backupEntries"),
                textOf(dialog, "backupChanges")));
        assertEquals(List.of(false, false, false), buttonDisabledStates(dialog));
    }

    private static Path createBackup(Path original, Path backupDirectory, String content) throws IOException {
        Path backup = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(original, BackupFileType.BACKUP, backupDirectory);
        return Files.writeString(backup, content);
    }

    private BackupResolverDialog createDialog(Path original, Path backupDirectory, CurrentThreadTaskExecutor taskExecutor) {
        AtomicReference<BackupResolverDialog> dialog = new AtomicReference<>();
        interact(() -> dialog.set(new BackupResolverDialog(
                original,
                backupDirectory,
                mock(ExternalApplicationsPreferences.class),
                importFormatPreferences(),
                taskExecutor)));
        return dialog.get();
    }

    private static ImportFormatPreferences importFormatPreferences() {
        ImportFormatPreferences preferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.bibEntryPreferences()).thenReturn(new BibEntryPreferences(','));
        return preferences;
    }

    private static String formatModified(Path path) throws IOException {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                .withZone(ZoneId.systemDefault())
                                .format(Files.getLastModifiedTime(path).toInstant());
    }

    private static String textOf(BackupResolverDialog dialog, String id) {
        return assertInstanceOf(Labeled.class, dialog.getDialogPane().lookup("#" + id)).getText();
    }

    private static List<Boolean> buttonDisabledStates(BackupResolverDialog dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        return List.of(
                dialogPane.lookupButton(BackupResolverDialog.RESTORE_FROM_BACKUP).isDisabled(),
                dialogPane.lookupButton(BackupResolverDialog.REVIEW_BACKUP).isDisabled(),
                dialogPane.lookupButton(BackupResolverDialog.IGNORE_BACKUP).isDisabled());
    }

    private static String stylesheet(String path) {
        return Objects.requireNonNull(BackupResolverDialogTest.class.getResource(path)).toExternalForm();
    }
}
