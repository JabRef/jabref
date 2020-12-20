package org.jabref.gui.externalfiles;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;

import org.jabref.gui.util.BaseDialog;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class ExternalFilesDialogView extends BaseDialog<Void> {

    @FXML private TextField directoryPathField;
    @FXML private ComboBox<FileChooser.ExtensionFilter> fileTypeSelection;
    @FXML private TreeView<?> tree;
    @Inject private PreferencesService preferencesService;

    public ExternalFilesDialogView() {

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
