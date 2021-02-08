package org.jabref.gui.preferences.table;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class TableTab extends AbstractPreferenceTabView<TableTabViewModel> implements PreferencesTab {

    @FXML private TableView<MainTableColumnModel> columnsList;
    @FXML private TableColumn<MainTableColumnModel, String> nameColumn;
    @FXML private TableColumn<MainTableColumnModel, String> actionsColumn;
    @FXML private ComboBox<MainTableColumnModel> addColumnName;
    @FXML private CheckBox specialFieldsEnable;
    @FXML private Button specialFieldsHelp;
    @FXML private RadioButton specialFieldsSyncKeywords;
    @FXML private RadioButton specialFieldsSerialize;
    @FXML private CheckBox extraFileColumnsEnable;
    @FXML private CheckBox autoResizeColumns;

    @FXML private RadioButton namesNatbib;
    @FXML private RadioButton nameAsIs;
    @FXML private RadioButton nameFirstLast;
    @FXML private RadioButton nameLastFirst;
    @FXML private RadioButton abbreviationDisabled;
    @FXML private RadioButton abbreviationEnabled;
    @FXML private RadioButton abbreviationLastNameOnly;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public TableTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry table");
    }

    public void initialize() {
        this.viewModel = new TableTabViewModel(dialogService, preferencesService);

        setupTable();
        setupBindings();

        ActionFactory actionFactory = new ActionFactory(preferencesService.getKeyBindingRepository());
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
        columnsList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                viewModel.removeColumn(columnsList.getSelectionModel().getSelectedItem());
                event.consume();
            }
        });

        columnsList.itemsProperty().bind(viewModel.columnsListProperty());

        new ViewModelListCellFactory<MainTableColumnModel>()
                .withText(MainTableColumnModel::getDisplayName)
                .install(addColumnName);
        addColumnName.itemsProperty().bind(viewModel.availableColumnsProperty());
        addColumnName.valueProperty().bindBidirectional(viewModel.addColumnProperty());
        addColumnName.setConverter(TableTabViewModel.columnNameStringConverter);
        addColumnName.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
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
        autoResizeColumns.selectedProperty().bindBidirectional(viewModel.autoResizeColumnsProperty());

        namesNatbib.selectedProperty().bindBidirectional(viewModel.namesNatbibProperty());
        nameAsIs.selectedProperty().bindBidirectional(viewModel.nameAsIsProperty());
        nameFirstLast.selectedProperty().bindBidirectional(viewModel.nameFirstLastProperty());
        nameLastFirst.selectedProperty().bindBidirectional(viewModel.nameLastFirstProperty());

        abbreviationDisabled.selectedProperty().bindBidirectional(viewModel.abbreviationDisabledProperty());
        abbreviationDisabled.disableProperty().bind(namesNatbib.selectedProperty().or(nameAsIs.selectedProperty()));
        abbreviationEnabled.selectedProperty().bindBidirectional(viewModel.abbreviationEnabledProperty());
        abbreviationEnabled.disableProperty().bind(namesNatbib.selectedProperty().or(nameAsIs.selectedProperty()));
        abbreviationLastNameOnly.selectedProperty().bindBidirectional(viewModel.abbreviationLastNameOnlyProperty());
        abbreviationLastNameOnly.disableProperty().bind(namesNatbib.selectedProperty().or(nameAsIs.selectedProperty()));
    }

    public void updateToCurrentColumnOrder() {
        viewModel.fillColumnList();
    }

    public void sortColumnUp() {
        viewModel.moveColumnUp();
    }

    public void sortColumnDown() {
        viewModel.moveColumnDown();
    }

    public void addColumn() {
        viewModel.insertColumnInList();
    }
}
