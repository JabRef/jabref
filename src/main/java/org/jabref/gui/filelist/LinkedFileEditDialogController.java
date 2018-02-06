package org.jabref.gui.filelist;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.AbstractController;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.model.entry.LinkedFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LinkedFileEditDialogController extends AbstractController<FileListDialogViewModel> {

    private static final Log LOGGER = LogFactory.getLog(LinkedFileEditDialogController.class);

    @FXML private TextField link;
    @FXML private TextField description;
    @FXML private ComboBox<ExternalFileType> fileType;

    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;
    @Inject private LinkedFile linkedFile;

    @FXML
    private void initialize() {
        viewModel = new FileListDialogViewModel(linkedFile, stateManager.getActiveDatabase().get(), dialogService);
        fileType.itemsProperty().bindBidirectional(viewModel.externalFileTypeProperty());
        description.textProperty().bindBidirectional(viewModel.descriptionProperty());
        link.textProperty().bindBidirectional(viewModel.linkProperty());
        fileType.valueProperty().bindBidirectional(viewModel.getSelectedExternalFileType());
    }

    @FXML
    private void openBrowseDialog(ActionEvent event) {
        viewModel.openBrowseDialog();
        link.requestFocus();
    }
}
