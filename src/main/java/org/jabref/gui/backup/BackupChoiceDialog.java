package org.jabref.gui.backup;

import java.nio.file.Path;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

public class BackupChoiceDialog extends BaseDialog<BackupChoiceDialogRecord> {
    public static final ButtonType RESTORE_BACKUP = new ButtonType(Localization.lang("Restore from backup"), ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType IGNORE_BACKUP = new ButtonType(Localization.lang("Ignore backup"), ButtonBar.ButtonData.CANCEL_CLOSE);
    public static final ButtonType REVIEW_BACKUP = new ButtonType(Localization.lang("Review backup"), ButtonBar.ButtonData.LEFT);

    private final ObservableList<BackupEntry> tableData = FXCollections.observableArrayList();

    @FXML
    private final TableView<BackupEntry> backupTableView;

    public BackupChoiceDialog(Path originalPath, Path backupDir) {
        setTitle(Localization.lang("Choose backup file"));
        setHeaderText(null);
        getDialogPane().setMinHeight(150);
        getDialogPane().setMinWidth(450);
        getDialogPane().getButtonTypes().setAll(RESTORE_BACKUP, IGNORE_BACKUP, REVIEW_BACKUP);

        String content = Localization.lang("It looks like JabRef did not shut down cleanly last time the file was used.") + "\n\n" +
                Localization.lang("Do you want to recover the library from a backup file?");

        backupTableView = new TableView<>();
        setupBackupTableView();
        pushSampleData();
        backupTableView.setItems(tableData);

        VBox contentBox = new VBox();
        contentBox.getChildren().addAll(new Label(content), backupTableView);
        contentBox.setPrefWidth(380);

        getDialogPane().setContent(contentBox);
        setResultConverter(dialogButton -> {
            if (dialogButton == RESTORE_BACKUP || dialogButton == REVIEW_BACKUP) {
                return new BackupChoiceDialogRecord(backupTableView.getSelectionModel().getSelectedItem(), dialogButton);
            }
            return new BackupChoiceDialogRecord(null, dialogButton);
        });
    }

    private void setupBackupTableView() {
        TableColumn<BackupEntry, String> dateColumn = new TableColumn<>(Localization.lang("Date of Backup"));
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());

        TableColumn<BackupEntry, String> sizeColumn = new TableColumn<>(Localization.lang("Size of Backup"));
        sizeColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());

        TableColumn<BackupEntry, Integer> entriesColumn = new TableColumn<>(Localization.lang("Number of Entries"));
        entriesColumn.setCellValueFactory(cellData -> cellData.getValue().entriesProperty().asObject());

        backupTableView.getColumns().addAll(dateColumn, sizeColumn, entriesColumn);
    }

    private void pushSampleData() {
        for (int i = 0; i < 50; i++) {
            tableData.add(new BackupEntry("2023-11-" + (i + 1), i + " MB", i));
        }
    }
}
