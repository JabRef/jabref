package org.jabref.gui.metadata;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;

import org.jabref.gui.icon.IconTheme.JabRefIcons;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class BibtexStringEditorDialogView extends BaseDialog<Void> {

    @FXML private Button btnNewString;
    @FXML private Button btnRemove;
    @FXML private Button btnHelp;
    @FXML private ButtonType saveButton;

    @FXML private TextField stringLabel;
    @FXML private TableView<BibtexStringViewModel> tblStrings;
    @FXML private TableColumn<BibtexStringViewModel, String> colLabel;
    @FXML private TableColumn<BibtexStringViewModel, String> colContent;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
    private final BibtexStringEditorDialogViewModel viewModel;

    public BibtexStringEditorDialogView(BibDatabase database) {
        viewModel = new BibtexStringEditorDialogViewModel(database);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        Button btnSave = (Button) this.getDialogPane().lookupButton(saveButton);

        btnSave.disableProperty().bind(viewModel.validProperty().not());

        setResultConverter(btn -> {
            if (saveButton.equals(btn)) {
                viewModel.save();
            }
            return null;
        });

        setTitle(Localization.lang("Strings for library"));
    }

    @FXML
    private void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());

        btnHelp.setGraphic(JabRefIcons.HELP.getGraphicNode());
        btnHelp.setTooltip(new Tooltip(Localization.lang("Open Help page")));

        btnNewString.setTooltip(new Tooltip(Localization.lang("New string")));
        btnRemove.setTooltip(new Tooltip(Localization.lang("Remove selected strings")));

        colLabel.setCellValueFactory(cellData -> cellData.getValue().getLabel());
        colLabel.setCellFactory(column -> new TextFieldTableCell<BibtexStringViewModel, String>(new DefaultStringConverter()) {

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty && (getTableRow() != null)) {
                    Object rowItem = getTableRow().getItem();

                    if ((rowItem != null) && (rowItem instanceof BibtexStringViewModel)) {
                        BibtexStringViewModel vm = (BibtexStringViewModel) rowItem;
                        visualizer.initVisualization(vm.labelValidation(), this);
                    }
                }
            }
        });

        colContent.setCellValueFactory(cellData -> cellData.getValue().getContent());
        colContent.setCellFactory(column -> new TextFieldTableCell<BibtexStringViewModel, String>(new DefaultStringConverter()) {

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty && (getTableRow() != null)) {
                    Object rowItem = getTableRow().getItem();

                    if ((rowItem != null) && (rowItem instanceof BibtexStringViewModel)) {
                        BibtexStringViewModel vm = (BibtexStringViewModel) rowItem;
                        visualizer.initVisualization(vm.contentValidation(), this);
                    }
                }

            }
        });

        colLabel.setOnEditCommit((CellEditEvent<BibtexStringViewModel, String> cell) -> {
            cell.getRowValue().setLabel(cell.getNewValue());
        });
        colContent.setOnEditCommit((CellEditEvent<BibtexStringViewModel, String> cell) -> {
            cell.getRowValue().setContent(cell.getNewValue());
        });

        tblStrings.itemsProperty().bindBidirectional(viewModel.allStringsProperty());
        tblStrings.setEditable(true);

        viewModel.seletedItemProperty().bind(tblStrings.getSelectionModel().selectedItemProperty());
        viewModel.newStringLabelProperty().bindBidirectional(stringLabel.textProperty());
    }

    @FXML
    private void addString(ActionEvent event) {
        viewModel.addNewString();
    }

    @FXML
    private void openHelp(ActionEvent event) {
        viewModel.openHelpPage();
    }

    @FXML
    private void removeString(ActionEvent event) {
        viewModel.removeString();
    }

}
