package org.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class ExportLinkedFilesAction extends AbstractAction {

    private final DialogService ds = new FXDialogService();
    private long totalFilesCount;
    private BibDatabaseContext databaseContext;
    private Optional<Path> exportPath = Optional.empty();
    private List<BibEntry> entries;

    public ExportLinkedFilesAction() {
        super(Localization.lang("Copy attached files to folder..."));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(Paths.get(Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY)))
                .build();
        entries = JabRefGUI.getMainFrame().getCurrentBasePanel().getSelectedEntries();
        exportPath = DefaultTaskExecutor
                .runInJavaFXThread(() -> ds.showDirectorySelectionDialog(dirDialogConfiguration));

        exportPath.ifPresent(path -> {
            databaseContext = JabRefGUI.getMainFrame().getCurrentBasePanel().getDatabaseContext();

            Service<List<CopyFilesResult>> exportService = new ExportLinkedFilesService(databaseContext, entries, path);
            startServiceAndshowProgessDialog(exportService);
        });
    }

    private void startServiceAndshowProgessDialog(Service<List<CopyFilesResult>> exportService) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            exportService.setOnSucceeded(value -> {
                System.out.println(exportService.getValue());

                System.out.println("Service succeeded");
                DefaultTaskExecutor.runInJavaFXThread(() -> showDialog(exportService.getValue()));

            });
            exportService.start();
            DialogService dlgService = new FXDialogService();
            dlgService.showCanceableProgressDialogAndWait(exportService);

        });
    }

    private void showDialog(List<CopyFilesResult> data) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Results");
        ObservableList<CopyFilesResult> tableData = FXCollections.observableArrayList(data);

        TableView<CopyFilesResult> tv = createTable();
        ScrollPane sp = new ScrollPane();
        sp.setContent(tv);

        dlg.getDialogPane().setContent(sp);
        tv.setItems(tableData);

        sp.setFitToHeight(true);
        sp.setFitToWidth(true);

        dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dlg.setResizable(true);
        dlg.showAndWait();
    }

    private static TableView<CopyFilesResult> createTable() {
        TableView<CopyFilesResult> tv = new TableView<>();

        TableColumn<CopyFilesResult, String> colFile = new TableColumn<>("File");
        TableColumn<CopyFilesResult, Boolean> colSuccess = new TableColumn<>("Success");
        TableColumn<CopyFilesResult, String> colMessage = new TableColumn<>("Log message");

        colFile.setCellValueFactory(cellData -> cellData.getValue().getFile());
        colSuccess.setCellValueFactory(cellData -> cellData.getValue().getSucess());
        colMessage.setCellValueFactory(cellData -> cellData.getValue().getMessage());

        tv.getColumns().add(colFile);
        tv.getColumns().add(colSuccess);
        tv.getColumns().add(colMessage);

        return tv;
    }

}
