package org.jabref.gui.copyfiles;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import org.jabref.gui.AbstractController;
import org.jabref.gui.util.ValueTableCellFactory;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory;

public class CopyFilesDialogController extends AbstractController<CopyFilesDialogViewModel> {

    @FXML private TableView<CopyFilesResultItemViewModel> tvResult;
    @FXML private TableColumn<CopyFilesResultItemViewModel, MaterialDesignIcon> colStatus;
    @FXML private TableColumn<CopyFilesResultItemViewModel, String> colMessage;
    @FXML private TableColumn<CopyFilesResultItemViewModel, String> colFile;

    @Inject private CopyFilesResultListDependency copyfilesresultlistDependency; //This var must have the same name as the key in the View

    @FXML
    private void close(@SuppressWarnings("unused") ActionEvent event) {
        getStage().close();
    }

    @FXML
    private void initialize() {
        viewModel = new CopyFilesDialogViewModel(copyfilesresultlistDependency);
        setupTable();
    }

    private void setupTable() {
        colFile.setCellValueFactory(cellData -> cellData.getValue().getFile());
        colMessage.setCellValueFactory(cellData -> cellData.getValue().getMessage());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().getIcon());

        colFile.setCellFactory(new ValueTableCellFactory<CopyFilesResultItemViewModel, String>().withText(item -> item).withTooltip(item -> item));
        colStatus.setCellFactory(new ValueTableCellFactory<CopyFilesResultItemViewModel, MaterialDesignIcon>().withGraphic(item -> {

            Text icon = MaterialDesignIconFactory.get().createIcon(item);
            if (item == MaterialDesignIcon.CHECK) {
                icon.setFill(Color.GREEN);
            }
            if (item == MaterialDesignIcon.ALERT) {
                icon.setFill(Color.RED);
            }
            return icon;
        }));

        tvResult.setItems(viewModel.copyFilesResultListProperty());
        tvResult.setColumnResizePolicy((param) -> true);
    }
}
