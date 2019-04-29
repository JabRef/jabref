package org.jabref.gui.preferences;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.model.entry.specialfields.SpecialField;

import com.airhacks.afterburner.views.ViewLoader;
import org.controlsfx.control.CheckListView;

public class TableColumnsTabView extends GridPane {

    @FXML private CheckListView<SpecialField> specialColumnsList;
    @FXML private ToggleGroup toggleDoiUrl;
    @FXML private ToggleGroup toggleSyncKeywords;
    @FXML private TextField name;
    @FXML private Button addButton;
    @FXML private CheckListView<ExternalFileTypes> externalFillesTypesList;
    private TableColumnsTabViewModel viewModel;

    public TableColumnsTabView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new TableColumnsTabViewModel();
        viewModel.enteredNameProperty().bind(name.textProperty());
    }

    @FXML
    void addColumn(ActionEvent event) {
        viewModel.addNewColumn();
    }

    @FXML
    void deleteSelectedColumn(ActionEvent event) {

    }

    @FXML
    void updateToCurrentColOrder(ActionEvent event) {

    }

    public void storeSettings() {

    }

}
