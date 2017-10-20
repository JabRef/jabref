package org.jabref.gui.copyfiles;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.AbstractController;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;

public class CopyFilesDialogController extends AbstractController<CopyFilesDialogViewModel> {

    @FXML private TableView<CopyFilesResultItemViewModel> tvResult;

    @FXML private TableColumn<CopyFilesResultItemViewModel, MaterialDesignIcon> colStatus;

    @FXML private TableColumn<CopyFilesResultItemViewModel, String> colMessage;

    @FXML private TableColumn<CopyFilesResultItemViewModel, String> colFile;

    @FXML
    void close(ActionEvent event) {
        getStage().close();
    }

    @FXML
    private void initialize() {
        viewModel = new CopyFilesDialogViewModel();
    }
}
