package org.jabref.gui.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
import org.jabref.logic.bibtex.comparator.BackupComparator;
import org.jabref.logic.bibtex.comparator.BackupComparison;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackupFileType;
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
    private static final String UNKNOWN = "–";

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
                Optional.empty(),
                Optional.empty(),
                mock(ExternalApplicationsPreferences.class));
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

        BackupResolverDialog dialog = createDialog(original, backup.getParent());

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

        BackupResolverDialog dialog = createDialog(original, tempDir.resolve("missing-backups"));

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

        BackupResolverDialog dialog = createDialog(original, backup.getParent());

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

        BackupResolverDialog dialog = createDialog(original, backup.getParent());

        assertEquals(List.of(UNKNOWN, "1", UNKNOWN), List.of(
                textOf(dialog, "currentEntries"),
                textOf(dialog, "backupEntries"),
                textOf(dialog, "backupChanges")));
    }

    @Test
    void failedComparisonShowsUnknownValues(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "@Article{current}");
        Path backup = createBackup(original, tempDir.resolve("backups"), "@Article{backup}");

        AtomicReference<BackupResolverDialog> dialog = new AtomicReference<>();
        interact(() -> dialog.set(new BackupResolverDialog(original, Optional.of(backup), Optional.empty(), mock(ExternalApplicationsPreferences.class))));

        assertEquals(List.of(UNKNOWN, UNKNOWN, UNKNOWN), List.of(
                textOf(dialog.get(), "currentEntries"),
                textOf(dialog.get(), "backupEntries"),
                textOf(dialog.get(), "backupChanges")));
        assertEquals(List.of(false, false, false), buttonDisabledStates(dialog.get()));
    }

    // [utest->req~jabgui.backup.restore-dialog-comparison~1]
    @Test
    void explainsWhatRestoreAndIgnoreDo() {
        awaitEvents();

        AtomicReference<List<String>> texts = new AtomicReference<>();
        interact(() -> texts.set(backupResolverDialog.getDialogPane().getContent().lookupAll(".label").stream()
                                                     .map(node -> ((Labeled) node).getText())
                                                     .toList()));

        assertTrue(texts.get().contains("Restoring overwrites the library file with the backup. Ignoring opens the library file as it is and keeps the backup file."),
                "Dialog texts: " + texts.get());
    }

    private static Path createBackup(Path original, Path backupDirectory, String content) throws IOException {
        Path backup = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(original, BackupFileType.BACKUP, backupDirectory);
        return Files.writeString(backup, content);
    }

    /// Computes the comparison like [org.jabref.gui.dialogs.BackupUIManager] does before it shows the dialog.
    private BackupResolverDialog createDialog(Path original, Path backupDirectory) {
        Optional<Path> backup = BackupFileUtil.getPathOfLatestExistingBackupFile(original, backupDirectory);
        Optional<BackupComparison> comparison = backup.flatMap(path -> {
            try {
                return Optional.of(BackupComparator.compare(original, path, importFormatPreferences()));
            } catch (IOException e) {
                return Optional.empty();
            }
        });
        AtomicReference<BackupResolverDialog> dialog = new AtomicReference<>();
        interact(() -> dialog.set(new BackupResolverDialog(
                original,
                backup,
                comparison,
                mock(ExternalApplicationsPreferences.class))));
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
