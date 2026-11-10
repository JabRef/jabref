package org.jabref.gui.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;
import java.util.OptionalInt;

import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.FXDialog;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.bibtex.comparator.BackupComparison;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupResolverDialog extends FXDialog {
    public static final ButtonType RESTORE_FROM_BACKUP = new ButtonType(Localization.lang("Restore from backup"), ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType REVIEW_BACKUP = new ButtonType(Localization.lang("Review backup"), ButtonBar.ButtonData.LEFT);
    public static final ButtonType IGNORE_BACKUP = new ButtonType(Localization.lang("Ignore backup"), ButtonBar.ButtonData.CANCEL_CLOSE);

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupResolverDialog.class);
    private static final String UNKNOWN = "–";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());

    // [impl->req~jabgui.backup.restore-dialog-comparison~1]
    public BackupResolverDialog(Path originalPath,
                                Optional<Path> backupPath,
                                Optional<BackupComparison> comparison,
                                ExternalApplicationsPreferences externalApplicationsPreferences) {
        super(AlertType.CONFIRMATION, Localization.lang("Backup found"), true);
        setHeaderText(null);
        getDialogPane().getButtonTypes().setAll(RESTORE_FROM_BACKUP, REVIEW_BACKUP, IGNORE_BACKUP);

        Node backupNameCell = backupPath.<Node>map(path -> backupLink(path, externalApplicationsPreferences))
                                        .orElseGet(() -> value("backupName", Localization.lang("File not found")));
        GridPane table = new GridPane();
        table.setHgap(12);
        table.setVgap(6);
        table.addRow(0, header(Localization.lang("Property")), header(Localization.lang("Current file")), header(Localization.lang("Backup file")));
        table.addRow(1, new Label(Localization.lang("Name")),
                capped(value("currentName", originalPath.getFileName().toString())),
                backupNameCell);
        table.addRow(2, new Label(Localization.lang("Size")),
                value("currentSize", FileUtil.getFileSize(originalPath).orElse(UNKNOWN)),
                value("backupSize", backupPath.flatMap(FileUtil::getFileSize).orElse(UNKNOWN)));
        table.addRow(3, new Label(Localization.lang("Last modified")),
                value("currentModified", formatModified(originalPath)),
                value("backupModified", backupPath.map(BackupResolverDialog::formatModified).orElse(UNKNOWN)));
        table.addRow(4, new Label(Localization.lang("Number of entries")),
                value("currentEntries", comparison.map(result -> format(result.currentEntryCount())).orElse(UNKNOWN)),
                value("backupEntries", comparison.map(result -> format(result.backupEntryCount())).orElse(UNKNOWN)));
        table.addRow(5, new Label(Localization.lang("Changes")),
                new Label(),
                value("backupChanges", comparison.flatMap(BackupComparison::changes).map(BackupResolverDialog::describeChanges).orElse(UNKNOWN)));

        VBox content = new VBox(10,
                wrapped(Localization.lang("A newer backup of this library was found.")),
                wrapped(Localization.lang("This could indicate that JabRef did not shut down cleanly last time the file was used.")),
                table,
                wrapped(Localization.lang("Restoring overwrites the library file with the backup. Ignoring opens the library file as it is and keeps the backup file.")),
                wrapped(Localization.lang("Do you want to recover the library from the backup file?")));
        getDialogPane().setContent(content);
    }

    private static Label header(String text) {
        Label headerLabel = new Label(text);
        headerLabel.getStyleClass().add("bold");
        return headerLabel;
    }

    private static Label value(String id, String text) {
        Label valueLabel = new Label(text);
        valueLabel.setId(id);
        return valueLabel;
    }

    /// Backup file names carry a hash prefix and a timestamp, so name cells are capped and show the full name as a tooltip.
    private static <T extends Labeled> T capped(T cell) {
        cell.setMaxWidth(240);
        cell.setTooltip(new Tooltip(cell.getText()));
        return cell;
    }

    private Hyperlink backupLink(Path backupPath, ExternalApplicationsPreferences externalApplicationsPreferences) {
        Hyperlink link = capped(new Hyperlink(backupPath.getFileName().toString()));
        link.setId("backupName");
        link.setOnAction(_ -> {
            try {
                NativeDesktop.openFolderAndSelectFile(backupPath, externalApplicationsPreferences, null);
            } catch (IOException ex) {
                LOGGER.error("Could not open backup folder", ex);
            }
        });
        return link;
    }

    private static String formatModified(Path file) {
        try {
            return DATE_TIME_FORMATTER.format(Files.getLastModifiedTime(file).toInstant());
        } catch (IOException ex) {
            LOGGER.debug("Could not read modification time of {}", file, ex);
            return UNKNOWN;
        }
    }

    private static String describeChanges(BackupComparison.ChangeCounts changes) {
        if (changes.added() == 0 && changes.removed() == 0 && changes.modified() == 0) {
            return Localization.lang("No entry changes");
        }
        return Localization.lang("removed: %0, added: %1, modified: %2",
                String.valueOf(changes.removed()), String.valueOf(changes.added()), String.valueOf(changes.modified()));
    }

    private static String format(OptionalInt count) {
        return count.isPresent() ? String.valueOf(count.orElseThrow()) : UNKNOWN;
    }

    private static Label wrapped(String text) {
        Label wrappedLabel = new Label(text);
        wrappedLabel.setWrapText(true);
        return wrappedLabel;
    }
}
