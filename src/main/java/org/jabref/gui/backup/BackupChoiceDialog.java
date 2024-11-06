package org.jabref.gui.backup;

import java.nio.file.Path;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import org.jabref.gui.FXDialog;
import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupChoiceDialog extends FXDialog {
    public static final ButtonType RESTORE_BACKUP = new ButtonType(Localization.lang("Restore from backup"), ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType IGNORE_BACKUP = new ButtonType(Localization.lang("Ignore backup"), ButtonBar.ButtonData.CANCEL_CLOSE);

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupResolverDialog.class);

    @FXML
    private TableView<DatabaseChange> backupTableView;

    public BackupChoiceDialog(Path originalPath, Path backupDir, ExternalApplicationsPreferences externalApplicationsPreferences) {
        super(AlertType.CONFIRMATION, Localization.lang("Choose backup"), true);
        setHeaderText(null);
        getDialogPane().setMinHeight(180);
        getDialogPane().getButtonTypes().setAll(RESTORE_BACKUP, IGNORE_BACKUP);

        Optional<Path> backupPathOpt = BackupFileUtil.getPathOfLatestExistingBackupFile(originalPath, BackupFileType.BACKUP, backupDir);
        String backupFilename = backupPathOpt.map(Path::getFileName).map(Path::toString).orElse(Localization.lang("File not found"));
        String content = Localization.lang("The :") + "\n" +
                Localization.lang("Here are some backup versions you can revert to");

        // Create a TableView for backups
        TableView<BackupEntry> backupTableView = new TableView<>();
        // Define columns
        TableColumn<BackupEntry, String> dateColumn = new TableColumn<>("Date of Backup");
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());

        TableColumn<BackupEntry, String> sizeColumn = new TableColumn<>("Size of Backup");
        sizeColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());

        TableColumn<BackupEntry, Integer> entriesColumn = new TableColumn<>("Number of Entries");
        entriesColumn.setCellValueFactory(cellData -> cellData.getValue().entriesProperty().asObject());

        // Add columns to the table
        backupTableView.getColumns().addAll(dateColumn, sizeColumn, entriesColumn);

        // Sample data
        ObservableList<BackupEntry> data = FXCollections.observableArrayList(
                new BackupEntry("2023-11-01", "500 MB", 120),
                new BackupEntry("2023-10-15", "300 MB", 80),
                new BackupEntry("2023-10-01", "250 MB", 60),
                new BackupEntry("2023-11-01", "500 MB", 120),
                new BackupEntry("2023-10-15", "300 MB", 80),
                new BackupEntry("2023-11-01", "500 MB", 120),
                new BackupEntry("2023-10-15", "300 MB", 80),
                new BackupEntry("2023-11-01", "500 MB", 120),
                new BackupEntry("2023-10-15", "300 MB", 80),
                new BackupEntry("2023-11-01", "500 MB", 120),
                new BackupEntry("2023-10-15", "300 MB", 80),
                new BackupEntry("2023-11-01", "500 MB", 120),
                new BackupEntry("2023-10-15", "300 MB", 80),
                new BackupEntry("2023-11-01", "500 MB", 120),
                new BackupEntry("2023-10-15", "300 MB", 80),
                new BackupEntry("2023-11-01", "500 MB", 120),
                new BackupEntry("2023-10-15", "300 MB", 80)
        );

        backupTableView.setItems(data);

        setContentText(content);

        // Create a VBox to hold the table and additional content
        VBox contentBox = new VBox(10);
        contentBox.getChildren().addAll(new Label(content), backupTableView);

        // Add the VBox to the dialog's content
        getDialogPane().setContent(contentBox);
    }
}

