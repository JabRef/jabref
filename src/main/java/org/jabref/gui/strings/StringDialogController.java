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
import javafx.util.converter.DefaultStringConverter;

import org.jabref.gui.AbstractController;
import org.jabref.gui.IconTheme.JabRefIcons;
import org.jabref.gui.StateManager;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

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
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();


    @FXML
    private void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());
        viewModel = new StringDialogViewModel(stateManager);

        btnHelp.setGraphic(JabRefIcons.HELP.getGraphicNode());
        btnHelp.setTooltip(new Tooltip(Localization.lang("Open Help page")));

        btnNewString.setGraphic(JabRefIcons.ADD.getGraphicNode());
        btnNewString.setTooltip(new Tooltip(Localization.lang("New string")));

        btnRemove.setGraphic(JabRefIcons.REMOVE.getGraphicNode());
        btnRemove.setTooltip(new Tooltip(Localization.lang("Remove string")));

        //Register TextfieldTableCell Control for validation
        // ValueExtractor.addObservableValueExtractor(c -> c instanceof TextFieldTableCell, c -> ((TextFieldTableCell<?, String>) c).textProperty());

        colLabel.setCellFactory(column -> {

            TextFieldTableCell<StringViewModel, String> cell = new TextFieldTableCell<>(new DefaultStringConverter());
            column.setCellValueFactory(cellData -> {

                visualizer.initVisualization(cellData.getValue().labelValidation(), cell);
                return cellData.getValue().getLabel();
            });
            return cell;

        });

        colContent.setCellFactory(column -> {

            TextFieldTableCell<StringViewModel, String> cell = new TextFieldTableCell<>(new DefaultStringConverter());
            column.setCellValueFactory(cellData -> {

                visualizer.initVisualization(cellData.getValue().contentValidation(), cell);
                return cellData.getValue().getContent();
            });
            return cell;

        });

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
