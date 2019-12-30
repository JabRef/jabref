package org.jabref.gui.preferences;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class TableColumnsTabView extends AbstractPreferenceTabView<TableColumnsTabViewModel> implements PreferencesTab {

    @FXML private TableView<MainTableColumnModel> columnsList;
    @FXML private TableColumn<MainTableColumnModel, String> nameColumn;
    @FXML private TableColumn<MainTableColumnModel, String> actionsColumn;
    @FXML private ComboBox<MainTableColumnModel> addColumnName;
    @FXML private CheckBox specialFieldsEnable;
    @FXML private Button specialFieldsHelp;
    @FXML private RadioButton specialFieldsSyncKeywords;
    @FXML private RadioButton specialFieldsSerialize;
    @FXML private CheckBox extraFileColumnsEnable;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    private final JabRefPreferences preferences;

    public TableColumnsTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry table columns");
    }

    public void initialize() {
        this.viewModel = new TableColumnsTabViewModel(dialogService, preferences);

        setupTable();
        setupBindings();

        ActionFactory actionFactory = new ActionFactory(preferences.getKeyBindingRepository());
        actionFactory.configureIconButton(StandardActions.HELP_SPECIAL_FIELDS, new HelpAction(HelpFile.SPECIAL_FIELDS), specialFieldsHelp);
    }

    private void setupTable() {
        nameColumn.setSortable(false);
        nameColumn.setReorderable(false);
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<MainTableColumnModel, String>()
                .withText(name -> name)
                .install(nameColumn);

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<MainTableColumnModel, String>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove column") + " " + name)
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.removeColumn(columnsList.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        viewModel.selectedColumnModelProperty().setValue(columnsList.getSelectionModel());
        columnsList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                viewModel.removeColumn(columnsList.getSelectionModel().getSelectedItem());
            }
        });

        columnsList.itemsProperty().bind(viewModel.columnsListProperty());

        new ViewModelListCellFactory<MainTableColumnModel>()
                .withText(MainTableColumnModel::getDisplayName)
                .install(addColumnName);
        addColumnName.itemsProperty().bind(viewModel.availableColumnsProperty());
        addColumnName.valueProperty().bindBidirectional(viewModel.addColumnProperty());
        addColumnName.setConverter(TableColumnsTabViewModel.columnNameStringConverter);
        addColumnName.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                viewModel.insertColumnInList();
                event.consume();
            }
        });

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.columnsListValidationStatus(), columnsList));
    }

    private void setupBindings() {
        specialFieldsEnable.selectedProperty().bindBidirectional(viewModel.specialFieldsEnabledProperty());
        specialFieldsSyncKeywords.selectedProperty().bindBidirectional(viewModel.specialFieldsSyncKeywordsProperty());
        specialFieldsSerialize.selectedProperty().bindBidirectional(viewModel.specialFieldsSerializeProperty());
        extraFileColumnsEnable.selectedProperty().bindBidirectional(viewModel.extraFileColumnsEnabledProperty());
    }

    public void updateToCurrentColumnOrder() { viewModel.fillColumnList(); }

    public void sortColumnUp() { viewModel.moveColumnUp(); }

    public void sortColumnDown() { viewModel.moveColumnDown(); }

    public void addColumn() { viewModel.insertColumnInList(); }

}
