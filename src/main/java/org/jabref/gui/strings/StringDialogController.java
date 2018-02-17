package org.jabref.gui.strings;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

import org.jabref.gui.AbstractController;
import org.jabref.gui.StateManager;

public class StringDialogController extends AbstractController<StringDialogViewModel> {

    @FXML private Button btnNewString;

    @FXML private Button btnRemove;

    @FXML private Button btnHelp;

    @FXML private TableView<StringViewModel> tblStrings;

    @FXML private TableColumn<StringViewModel, String> colLabel;

    @FXML private TableColumn<StringViewModel, String> colContent;

    @Inject private StateManager stateManager;

    @FXML
    private void initialize() {

        viewModel = new StringDialogViewModel(stateManager);
        colLabel.setCellValueFactory(cellData -> cellData.getValue().getLabel());
        colContent.setCellValueFactory(cellData -> cellData.getValue().getContent());

        colLabel.setCellFactory(TextFieldTableCell.<StringViewModel> forTableColumn());
        colContent.setCellFactory(TextFieldTableCell.<StringViewModel> forTableColumn());
        colLabel.setOnEditCommit(
                (CellEditEvent<StringViewModel, String> t) -> {
                    t.getTableView().getItems().get(
                            t.getTablePosition().getRow()).setLabel(t.getNewValue());
                });
        colContent.setOnEditCommit(
                (CellEditEvent<StringViewModel, String> t) -> {
                    t.getTableView().getItems().get(
                            t.getTablePosition().getRow()).setContent(t.getNewValue());
                });

        tblStrings.itemsProperty().bindBidirectional(viewModel.allStringsProperty());
        tblStrings.setEditable(true);
    }

    @FXML
    void addString(ActionEvent event) {

        viewModel.addNewString();

    }

    @FXML
    void openHelp(ActionEvent event) {

    }

    @FXML
    void removeString(ActionEvent event) {
        StringViewModel selected = tblStrings.getSelectionModel().getSelectedItem();
        viewModel.removeString(selected);
    }
}
