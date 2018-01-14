package org.jabref.gui.filelist;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.AbstractController;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.model.entry.LinkedFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileListDialogController extends AbstractController<FileListDialogViewModel> {

    private static final Log LOGGER = LogFactory.getLog(FileListDialogController.class);
    private boolean okPressed;

    @FXML private TextField tfLink;
    @FXML private Button btnBrowse;
    @FXML private Button btnOpen;
    @FXML private TextField tfDescription;
    @FXML private ComboBox<ExternalFileType> cmbFileType;
    @FXML private Button btnOk;
    @FXML private Button btnCancel;

    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;
    @Inject private FileListDialogOptionDependency dependency;

    @FXML
    private void initialize() {
        viewModel = new FileListDialogViewModel(stateManager.getActiveDatabase().get(), dialogService, dependency.getDialogOptions());
        setBindings();

        //TODO: is a bit too early I think
        /*     this.getStage().getScene().addEventHandler(WindowEvent.WINDOW_SHOWING, event -> {

            System.out.println("Window showing event");

        });
        System.out.println(dependency.getDialogOptions());
        */
    }

    private void setBindings() {
        cmbFileType.itemsProperty().bindBidirectional(viewModel.externalFileTypeProperty());
        tfDescription.textProperty().bindBidirectional(viewModel.descriptionProperty());
        tfLink.textProperty().bindBidirectional(viewModel.linkProperty());
        cmbFileType.valueProperty().bindBidirectional(viewModel.getSelectedExternalFileType());
    }

    @FXML
    private void browseFileDialog(ActionEvent event) {
        viewModel.browseFileDialog();
        tfLink.requestFocus();
    }

    @FXML
    private void cancel(ActionEvent event) {
        getStage().close();
    }

    @FXML
    private void ok_clicked(ActionEvent event) {
        viewModel.setOkPressed();
    }

    private void setValues(LinkedFile entry) {
        viewModel.setValues(entry);
    }

    @FXML
    void openFile(ActionEvent event) {

        ExternalFileType type = cmbFileType.getSelectionModel().getSelectedItem();
        if (type != null) {
            try {
                JabRefDesktop.openExternalFileAnyFormat(stateManager.getActiveDatabase().get(), viewModel.linkProperty().get(), Optional.of(type));
            } catch (IOException e) {
                LOGGER.error("File could not be opened", e);
            }
        }
    }
}
