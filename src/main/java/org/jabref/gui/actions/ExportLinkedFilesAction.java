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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

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

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory;

public class ExportLinkedFilesAction extends AbstractAction {

    private final DialogService dialogService = new FXDialogService();
    private BibDatabaseContext databaseContext;
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

        Optional<Path> exportPath = DefaultTaskExecutor
                .runInJavaFXThread(() -> dialogService.showDirectorySelectionDialog(dirDialogConfiguration));

        exportPath.ifPresent(path -> {
            databaseContext = JabRefGUI.getMainFrame().getCurrentBasePanel().getDatabaseContext();

            Service<List<CopyFilesResultViewModel>> exportService = new ExportLinkedFilesService(databaseContext, entries, path);
            startServiceAndshowProgessDialog(exportService);
        });
    }

    private void startServiceAndshowProgessDialog(Service<List<CopyFilesResultViewModel>> exportService) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            exportService.setOnSucceeded(value -> {
                DefaultTaskExecutor.runInJavaFXThread(() -> showDialog(exportService.getValue()));
            });
            exportService.start();
            DialogService dlgService = new FXDialogService();
            dlgService.showCanceableProgressDialogAndWait(exportService);

        });
    }

    private void showDialog(List<CopyFilesResultViewModel> data) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(Localization.lang("Result"));
        ObservableList<CopyFilesResultViewModel> tableData = FXCollections.observableArrayList(data);

        TableView<CopyFilesResultViewModel> tv = createTable();
        ScrollPane sp = new ScrollPane();
        sp.setContent(tv);

        dlg.getDialogPane().setContent(sp);
        tv.setItems(tableData);

        sp.setFitToHeight(true);
        sp.setFitToWidth(true);

        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.setResizable(true);
        dlg.showAndWait();
    }

    private static TableView<CopyFilesResultViewModel> createTable() {
        TableView<CopyFilesResultViewModel> tableResult = new TableView<>();

        TableColumn<CopyFilesResultViewModel, String> colFile = new TableColumn<>(Localization.lang("File"));
        TableColumn<CopyFilesResultViewModel, MaterialDesignIcon> colIcon = new TableColumn<>(Localization.lang("Status"));
        TableColumn<CopyFilesResultViewModel, String> colMessage = new TableColumn<>(Localization.lang("Message"));

        colFile.setCellValueFactory(cellData -> cellData.getValue().getFile());
        colMessage.setCellValueFactory(cellData -> cellData.getValue().getMessage());
        colIcon.setCellValueFactory(cellData -> cellData.getValue().getIcon());

        colIcon.setCellFactory(column -> {
            return new TableCell<CopyFilesResultViewModel, MaterialDesignIcon>() {

                @Override
                protected void updateItem(MaterialDesignIcon item, boolean empty) {
                    super.updateItem(item, empty);

                    if ((item == null) || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        Text icon = MaterialDesignIconFactory.get().createIcon(item);

                        //Green checkmark
                        if (item == MaterialDesignIcon.CHECK) {
                            icon.setFill(Color.GREEN);
                        }
                        //Red Alert symbol
                        if (item == MaterialDesignIcon.ALERT) {
                            icon.setFill(Color.RED);
                        }
                        setGraphic(icon);

                    }
                }
            };
        });
        tableResult.getColumns().add(colIcon);
        tableResult.getColumns().add(colMessage);
        tableResult.getColumns().add(colFile);

        return tableResult;
    }

}