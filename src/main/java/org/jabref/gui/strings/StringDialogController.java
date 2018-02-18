package org.jabref.gui.strings;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;

import org.jabref.gui.AbstractController;
import org.jabref.gui.IconTheme.JabRefIcons;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.gui.StateManager;

public class StringDialogController extends AbstractController<StringDialogViewModel> {

    @FXML private Button btnNewString;
    @FXML private Button btnRemove;
    @FXML private Button btnHelp;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;
    @FXML private TableView<StringViewModel> tblStrings;
    @FXML private TableColumn<StringViewModel, String> colLabel;
    @FXML private TableColumn<StringViewModel, String> colContent;
    @Inject private StateManager stateManager;

    @FXML
    private void initialize() {

        btnHelp.setGraphic(JabRefIcons.HELP.getGraphicNode());
        btnHelp.setTooltip(new Tooltip(Localization.lang("Open Help page")));

        btnNewString.setGraphic(JabRefIcons.ADD.getGraphicNode());
        btnNewString.setTooltip(new Tooltip(Localization.lang("New string")));

        btnRemove.setGraphic(JabRefIcons.REMOVE.getGraphicNode());
        btnRemove.setTooltip(new Tooltip(Localization.lang("Remove string")));

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

        tblStrings.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> viewModel.validateInput(newValue));
    }

    @FXML
    private void addString(ActionEvent event) {
        viewModel.addNewString();
    }

    @FXML
    private void openHelp(ActionEvent event) {
        HelpAction.openHelpPage(HelpFile.STRING_EDITOR);
    }

    @FXML
    private void removeString(ActionEvent event) {
        StringViewModel selected = tblStrings.getSelectionModel().getSelectedItem();
        viewModel.removeString(selected);
    }

    @FXML
    private void save() {
        viewModel.save();
    }

    @FXML
    private void close() {
        getStage().close();
    }
}
