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
import org.jabref.gui.StateManager;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import de.saxsys.mvvmfx.utils.validation.visualization.ValidationVisualizer;

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

        viewModel = new StringDialogViewModel(stateManager);

        btnHelp.setGraphic(JabRefIcons.HELP.getGraphicNode());
        btnHelp.setTooltip(new Tooltip(Localization.lang("Open Help page")));

        btnNewString.setGraphic(JabRefIcons.ADD.getGraphicNode());
        btnNewString.setTooltip(new Tooltip(Localization.lang("New string")));

        btnRemove.setGraphic(JabRefIcons.REMOVE.getGraphicNode());
        btnRemove.setTooltip(new Tooltip(Localization.lang("Remove string")));

        colLabel.setCellValueFactory(cellData -> cellData.getValue().getLabel());
        colContent.setCellValueFactory(cellData -> cellData.getValue().getContent());

        colLabel.setCellFactory(TextFieldTableCell.forTableColumn());
        colContent.setCellFactory(TextFieldTableCell.forTableColumn());
        //Register TextfieldTableCell Control for validation

        colLabel.setOnEditCommit(
                (CellEditEvent<StringViewModel, String> cell) -> {
                    cell.getRowValue().setLabel(cell.getNewValue());
                });
        colContent.setOnEditCommit(
                (CellEditEvent<StringViewModel, String> cell) -> {
                    cell.getRowValue().setContent(cell.getNewValue());
                });

        tblStrings.itemsProperty().bindBidirectional(viewModel.allStringsProperty());
        tblStrings.setEditable(true);

        ValidationVisualizer visualizer = new ControlsFxVisualizer();

        //The problemm is that the viewModel here is not the viewModel of the List
        //visualizer.initVisualization(viewModel.labelValidation(), colLabel, true);

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
