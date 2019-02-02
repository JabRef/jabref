package org.jabref.gui.strings;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
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

import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme.JabRefIcons;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.fxmisc.easybind.EasyBind;

public class StringDialogView extends BaseDialog<Void> {

    @FXML private Button btnNewString;
    @FXML private Button btnRemove;
    @FXML private Button btnHelp;
    @FXML private ButtonType saveButton;

    @FXML private TextField stringLabel;
    @FXML private TableView<StringViewModel> tblStrings;
    @FXML private TableColumn<StringViewModel, String> colLabel;
    @FXML private TableColumn<StringViewModel, String> colContent;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
    private final StringDialogViewModel viewModel;

    public StringDialogView(BibDatabase database) {
        viewModel = new StringDialogViewModel(database);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        Button btnSave = (Button) this.getDialogPane().lookupButton(saveButton);

        //Adapted from the EasyBind Tutorial: https://github.com/TomasMikula/EasyBind#example-disable-save-all-button-on-no-unsaved-changes
        //Disable the button, if any of the validators is not valid
        ObservableList<ObservableValue<Boolean>> allValidProperty = EasyBind.map(viewModel.allStringsProperty(), StringViewModel::combinedValidationValidProperty);
        ObservableValue<Boolean> anyNotValid = EasyBind.combine(allValidProperty, stream -> stream.anyMatch(valid -> !valid));
        btnSave.disableProperty().bind(anyNotValid);

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

        btnNewString.setGraphic(JabRefIcons.ADD.getGraphicNode());
        btnNewString.setTooltip(new Tooltip(Localization.lang("New string")));

        btnRemove.setGraphic(JabRefIcons.REMOVE.getGraphicNode());
        btnRemove.setTooltip(new Tooltip(Localization.lang("Remove selected strings")));

        colLabel.setCellValueFactory(cellData -> cellData.getValue().getLabel());
        colLabel.setCellFactory(column -> new TextFieldTableCell<StringViewModel, String>(new DefaultStringConverter()) {

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty) {
                    if (getTableRow() != null) {
                        Object rowItem = getTableRow().getItem();

                        if ((rowItem != null) && (rowItem instanceof StringViewModel)) {
                            StringViewModel vm = (StringViewModel) rowItem;

                            visualizer.initVisualization(vm.labelValidation(), this);
                        }
                    }
                }
            }
        });

        colContent.setCellValueFactory(cellData -> cellData.getValue().getContent());
        colContent.setCellFactory(column -> new TextFieldTableCell<StringViewModel, String>(new DefaultStringConverter()) {

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty) {
                    if (getTableRow() != null) {
                        Object rowItem = getTableRow().getItem();

                        if ((rowItem != null) && (rowItem instanceof StringViewModel)) {
                            StringViewModel vm = (StringViewModel) rowItem;

                            visualizer.initVisualization(vm.contentValidation(), this);
                        }
                    }
                }
            }
        });

        colLabel.setOnEditCommit((CellEditEvent<StringViewModel, String> cell) -> {
            cell.getRowValue().setLabel(cell.getNewValue());
        });
        colContent.setOnEditCommit((CellEditEvent<StringViewModel, String> cell) -> {
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
        HelpAction.openHelpPage(HelpFile.STRING_EDITOR);
    }

    @FXML
    private void removeString(ActionEvent event) {
        viewModel.removeString();
    }

}
