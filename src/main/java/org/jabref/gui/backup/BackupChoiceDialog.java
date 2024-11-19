package org.jabref.gui.backup;

import java.nio.file.Path;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

public class BackupChoiceDialog extends BaseDialog<BackupChoiceDialogRecord> {
    public static final ButtonType RESTORE_BACKUP = new ButtonType(Localization.lang("Restore from backup"), ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType IGNORE_BACKUP = new ButtonType(Localization.lang("Ignore backup"), ButtonBar.ButtonData.CANCEL_CLOSE);
    public static final ButtonType REVIEW_BACKUP = new ButtonType(Localization.lang("Review backup"), ButtonBar.ButtonData.LEFT);

    private static final int ROWS_PER_PAGE = 10;  // Define number of rows per page

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

        backupTableView = new TableView<BackupEntry>();
        setupBackupTableView();

        // Sample data
        ObservableList<BackupEntry> data = FXCollections.observableArrayList();
        for (int i = 0; i < 100; i++) {  // Adjust 20 to however many entries you want
            data.add(new BackupEntry("2023-11-01", i + " MB", i));
        }
        setContentText(content);

        // Pagination control
        int pageCount = (int) Math.ceil(data.size() / (double) ROWS_PER_PAGE);
        Pagination pagination = new Pagination(pageCount, 0);
        pagination.setPageFactory(pageIndex -> {
            int start = pageIndex * ROWS_PER_PAGE;
            int end = Math.min(start + ROWS_PER_PAGE, data.size());
            backupTableView.setItems(FXCollections.observableArrayList(data.subList(start, end)));
            backupTableView.getSelectionModel().selectFirst();
            return new VBox(backupTableView);
        });

        // VBox content to hold the pagination and the label
        VBox contentBox = new VBox(10);
        contentBox.getChildren().addAll(new Label(content), pagination);
        contentBox.setPrefWidth(380);

        // Set the dialog content
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
}

