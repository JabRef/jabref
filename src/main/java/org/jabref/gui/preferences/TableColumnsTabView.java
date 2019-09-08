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
import javafx.util.StringConverter;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class TableColumnsTabView extends AbstractPreferenceTabView implements PreferencesTab {

    @FXML private TableView<TableColumnsItemModel> columnsList;
    @FXML private TableColumn<TableColumnsItemModel, Field> nameColumn;
    @FXML private TableColumn<TableColumnsItemModel, Field> actionsColumn;
    @FXML private ComboBox<Field> addColumnName;
    @FXML private CheckBox showFileColumn;
    @FXML private CheckBox showUrlColumn;
    @FXML private RadioButton urlFirst;
    @FXML private RadioButton doiFirst;
    @FXML private CheckBox showEPrintColumn;
    @FXML private CheckBox specialFieldsEnable;
    @FXML private Button specialFieldsHelp;
    @FXML private RadioButton specialFieldsSyncKeywords;
    @FXML private RadioButton specialFieldsSerialize;
    @FXML private CheckBox extraFileColumnsEnable;

    private ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

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
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().fieldProperty());
        new ValueTableCellFactory<TableColumnsItemModel, Field>().withText(FieldsUtil::getNameWithType).install(nameColumn);

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().fieldProperty());
        new ValueTableCellFactory<TableColumnsItemModel, Field>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove column") + " " + name.getDisplayName())
                .withOnMouseClickedEvent(item -> evt ->
                        ((TableColumnsTabViewModel) viewModel).removeColumn(columnsList.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        ((TableColumnsTabViewModel) viewModel).selectedColumnModelProperty().setValue(columnsList.getSelectionModel());
        columnsList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                ((TableColumnsTabViewModel) viewModel).removeColumn(columnsList.getSelectionModel().getSelectedItem());
            }
        });

        columnsList.itemsProperty().bind(((TableColumnsTabViewModel) viewModel).columnsListProperty());

        new ViewModelListCellFactory<Field>()
                .withText(FieldsUtil::getNameWithType)
                .install(addColumnName);
        addColumnName.itemsProperty().bind(((TableColumnsTabViewModel) viewModel).availableColumnsProperty());
        addColumnName.valueProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).addColumnProperty());
        addColumnName.setConverter(new StringConverter<>() {
            @Override
            public String toString(Field object) {
                if (object != null) {
                    return object.getName();
                } else {
                    return "";
                }
            }

            @Override
            public Field fromString(String string) {
                return FieldFactory.parseField(string);
            }
        });

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(((TableColumnsTabViewModel) viewModel).columnsListValidationStatus(), columnsList));
    }

    private void setupBindings() {
        showFileColumn.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).showFileColumnProperty());
        showUrlColumn.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).showUrlColumnProperty());
        urlFirst.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).preferUrlProperty());
        doiFirst.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).preferDoiProperty());
        showEPrintColumn.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).showEPrintColumnProperty());
        specialFieldsEnable.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).specialFieldsEnabledProperty());
        specialFieldsSyncKeywords.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).specialFieldsSyncKeywordsProperty());
        specialFieldsSerialize.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).specialFieldsSerializeProperty());
        extraFileColumnsEnable.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).extraFileColumnsEnabledProperty());
    }

    public void updateToCurrentColumnOrder() { ((TableColumnsTabViewModel) viewModel).fillColumnList(); }

    public void sortColumnUp() { ((TableColumnsTabViewModel) viewModel).moveColumnUp(); }

    public void sortColumnDown() { ((TableColumnsTabViewModel) viewModel).moveColumnDown(); }

    public void addColumn() { ((TableColumnsTabViewModel) viewModel).insertColumnInList(); }

}
