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

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupChoiceDialog extends BaseDialog<BackupEntry> {
    public static final ButtonType RESTORE_BACKUP = new ButtonType(Localization.lang("Restore from backup"), ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType IGNORE_BACKUP = new ButtonType(Localization.lang("Ignore backup"), ButtonBar.ButtonData.CANCEL_CLOSE);

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupChoiceDialog.class);

    @FXML
    private TableView<DatabaseChange> backupTableView;

    public BackupChoiceDialog(Path originalPath, Path backupDir, ExternalApplicationsPreferences externalApplicationsPreferences) {
        setTitle(Localization.lang("Choose backup file"));
        setHeaderText(null);
        getDialogPane().setMinHeight(180);
        getDialogPane().setMinWidth(600);
        getDialogPane().getButtonTypes().setAll(RESTORE_BACKUP, IGNORE_BACKUP);

        Optional<Path> backupPathOpt = BackupFileUtil.getPathOfLatestExistingBackupFile(originalPath, BackupFileType.BACKUP, backupDir);
        String backupFilename = backupPathOpt.map(Path::getFileName).map(Path::toString).orElse(Localization.lang("File not found"));
        String content = Localization.lang("Here are some backup files you can revert to.");

        // Builds TableView
        TableView<BackupEntry> backupTableView = new TableView<>();

        TableColumn<BackupEntry, String> dateColumn = new TableColumn<>(Localization.lang("Date of Backup"));
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());

        TableColumn<BackupEntry, String> sizeColumn = new TableColumn<>(Localization.lang("Size of Backup"));
        sizeColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());

        TableColumn<BackupEntry, Integer> entriesColumn = new TableColumn<>(Localization.lang("Number of Entries"));
        entriesColumn.setCellValueFactory(cellData -> cellData.getValue().entriesProperty().asObject());

        backupTableView.getColumns().addAll(dateColumn, sizeColumn, entriesColumn);
        backupTableView.getSelectionModel().selectFirst();

        // Sample data
        ObservableList<BackupEntry> data = FXCollections.observableArrayList();
        for (int i = 0; i < 20; i++) {  // Adjust 20 to however many entries you want
            data.add(new BackupEntry("2023-11-01", String.valueOf(i) + " MB", i));
        }
        backupTableView.setItems(data);

        setContentText(content);

        // Create a VBox to hold the table and additional content
        VBox contentBox = new VBox(10);
        contentBox.getChildren().addAll(new Label(content), backupTableView);
        contentBox.setPrefWidth(380);
        // Add the VBox to the dialog's content
        getDialogPane().setContent(contentBox);

        setResultConverter(dialogButton -> {
            if (dialogButton == RESTORE_BACKUP) {
                return (BackupEntry) backupTableView.getSelectionModel().getSelectedItem();
            }
            return null;
        });
    }
}

