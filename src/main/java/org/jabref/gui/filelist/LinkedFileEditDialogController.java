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
import org.jabref.preferences.PreferencesService;

public class LinkedFileEditDialogController extends AbstractController<LinkedFilesEditDialogViewModel> {


    @FXML private TextField link;
    @FXML private TextField description;
    @FXML private ComboBox<ExternalFileType> fileType;

    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;
    @Inject private LinkedFilesWrapper linkedFilesWrapper;

    @Inject private PreferencesService preferences;

    @FXML
    private void initialize() {
        viewModel = new LinkedFilesEditDialogViewModel(linkedFilesWrapper.getLinkedFile(), stateManager.getActiveDatabase().get(), dialogService, preferences);
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
