package org.jabref.gui.metadata;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
    @FXML private TextField addStringLabel;
    @FXML private TextField addStringContent;
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
        labelColumn.setCellValueFactory(cellData -> cellData.getValue().labelProperty());
        new ViewModelTextFieldTableCellVisualizationFactory<BibtexStringEditorItemModel, String>()
                .withValidation(BibtexStringEditorItemModel::labelValidation, visualizer)
                .install(labelColumn, new DefaultStringConverter());
        labelColumn.setOnEditCommit((CellEditEvent<BibtexStringEditorItemModel, String> cell) -> {
            String newLabelValue = cell.getNewValue();
            if (viewModel.labelAlreadyExists(newLabelValue)) {
                dialogService.showErrorDialogAndWait(Localization.lang("A string with the label '%0' already exists.", newLabelValue));
                cell.getRowValue().setLabel(cell.getOldValue());
            } else {
                cell.getRowValue().setLabel(newLabelValue);
            }
        });

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

        addStringLabel.textProperty().bindBidirectional(viewModel.addLabelProperty());
        addStringLabel.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                addStringContent.requestFocus();
                addStringContent.selectAll();
                event.consume();
            }
        });

        addStringContent.textProperty().bindBidirectional(viewModel.addContentProperty());
        addStringContent.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                viewModel.addNewString();
                addStringLabel.requestFocus();
                event.consume();
            }
        });

        stringsList.itemsProperty().bindBidirectional(viewModel.stringsListProperty());
        stringsList.setEditable(true);
    }

    @FXML
    private void addString() {
        viewModel.addNewString();
    }

    @FXML
    private void openHelp() {
        viewModel.openHelpPage();
    }
}
