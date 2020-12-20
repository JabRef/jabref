package org.jabref.gui.externalfiles;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.BaseDialog;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class ExternalFilesDialogView extends BaseDialog<Void> {

    @FXML private TextField directoryPathField;
    @FXML private ComboBox<FileChooser.ExtensionFilter> fileTypeSelection;
    @FXML private TreeView<?> tree;
    @Inject private PreferencesService preferencesService;
    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;
    private final ExternalFilesDialogViewModel viewModel;

    public ExternalFilesDialogView() {

        viewModel = new ExternalFilesDialogViewModel(dialogService, null, ExternalFileTypes.getInstance(), null, null, preferencesService, stateManager);

        ViewLoader.view(this)
                  .load()
                  .setAsContent(this.getDialogPane());
    }

    @FXML
    private void initialize() {

    }

    @FXML
    void browseFileDirectory(ActionEvent event) {

    }

    @FXML
    void collapseAll(ActionEvent event) {

    }

    @FXML
    void expandAll(ActionEvent event) {

    }

    @FXML
    void scanFiles(ActionEvent event) {

    }

    @FXML
    void selectAll(ActionEvent event) {

    }

    @FXML
    void unselectAll(ActionEvent event) {

    }

}
