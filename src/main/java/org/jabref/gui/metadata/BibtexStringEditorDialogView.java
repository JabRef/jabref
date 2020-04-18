package org.jabref.gui.metadata;

import java.util.Optional;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.util.converter.DefaultStringConverter;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTextFieldTableCellVisualizationFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class BibtexStringEditorDialogView extends BaseDialog<Void> {

    @FXML private TableView<BibtexStringEditorItemModel> stringsList;
    @FXML private TableColumn<BibtexStringEditorItemModel, String> labelColumn;
    @FXML private TableColumn<BibtexStringEditorItemModel, String> contentColumn;
    @FXML private TableColumn<BibtexStringEditorItemModel, String> actionsColumn;
    @FXML private Button addStringButton;
    @FXML private ButtonType saveButton;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
    private final BibtexStringEditorDialogViewModel viewModel;

    @Inject private DialogService dialogService;

    public BibtexStringEditorDialogView(BibDatabase database) {
        this.viewModel = new BibtexStringEditorDialogViewModel(database);

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

        addStringButton.setTooltip(new Tooltip(Localization.lang("New string")));

        labelColumn.setSortable(true);
        labelColumn.setReorderable(false);

        // The ErrorDialog has to be opened from the CellValueFactory, as it throws errors otherwise. The Implementation
        // follows basically the SpinnerValueFactory.
        // FixMe: Possible scenario: If the newLabel and the oldLabel are both in use the the listener jumps endlessly
        //  between the same change back and forth
        labelColumn.setCellValueFactory(cellData -> {
            cellData.getValue().labelProperty().addListener(((observable, oldLabel, newLabel) -> {
                Optional<BibtexStringEditorItemModel> item = viewModel.labelAlreadyExists(newLabel);
                if (item.isPresent() && !item.get().equals(stringsList.getFocusModel().getFocusedItem())) {
                    dialogService.showErrorDialogAndWait(Localization.lang("A string with the label '%0' already exists.", newLabel));
                    cellData.getValue().setLabel(oldLabel);
                } else {
                    cellData.getValue().setLabel(newLabel);
                }
            }));
            return cellData.getValue().labelProperty();
        });
        new ViewModelTextFieldTableCellVisualizationFactory<BibtexStringEditorItemModel, String>()
                .withValidation(BibtexStringEditorItemModel::labelValidation, visualizer)
                .install(labelColumn, new DefaultStringConverter());

        contentColumn.setSortable(true);
        contentColumn.setReorderable(false);
        contentColumn.setCellValueFactory(cellData -> cellData.getValue().contentProperty());
        new ViewModelTextFieldTableCellVisualizationFactory<BibtexStringEditorItemModel, String>()
                .withValidation(BibtexStringEditorItemModel::contentValidation, visualizer)
                .install(contentColumn, new DefaultStringConverter());
        contentColumn.setOnEditCommit((CellEditEvent<BibtexStringEditorItemModel, String> cell) ->
                cell.getRowValue().setContent(cell.getNewValue()));

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().labelProperty());
        new ValueTableCellFactory<BibtexStringEditorItemModel, String>()
                .withGraphic(label -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(label -> Localization.lang("Remove string %0", label))
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.removeString(stringsList.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        stringsList.itemsProperty().bindBidirectional(viewModel.stringsListProperty());
        stringsList.setEditable(true);

        // ToDo: Some keyPressed-Events should be added to navigate in the table, at least tab.
    }

    /**
     * Inserts a StringConstant into the stringsList and automatically starts the edit mode for it's label.
     *
     * ToDo: The second to the fifth line added does not automatically start edit mode, still needs debugging
     */
    @FXML
    private void addString() {
        viewModel.addNewString();
        stringsList.getFocusModel().focus(stringsList.getItems().size() - 1, labelColumn);
        stringsList.edit(stringsList.getItems().size() - 1, labelColumn);
    }

    @FXML
    private void openHelp() {
        viewModel.openHelpPage();
    }
}
