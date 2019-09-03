package org.jabref.gui.preferences;

import javax.inject.Inject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
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
    @FXML private Button reloadTableColumns;
    @FXML private Button sortColumnUp;
    @FXML private Button sortColumnDown;
    @FXML private Button addColumn;
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

    @Inject private DialogService dialogService;
    private final JabRefPreferences preferences;
    private final JabRefFrame frame;

    public TableColumnsTabView(JabRefPreferences preferences, JabRefFrame frame) {
        this.preferences = preferences;
        this.frame = frame;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry table columns");
    }

    public void initialize() {
        this.viewModel = new TableColumnsTabViewModel(dialogService, preferences, frame);

        setupTable();
        setupBindings();
        setupIconButtons();
    }

    private void setupTable() {
        nameColumn.setSortable(false);
        nameColumn.setReorderable(false);
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().fieldProperty());
        nameColumn.setCellFactory(cellData -> new TableCell<>() {
            @Override
            public void updateItem(Field item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    setText("");
                } else {
                    setText(((TableColumnsTabViewModel) viewModel).getFieldDisplayName(item));
                }
            }

            private String getString() {
                return getItem() == null ? "" : getItem().getName();
            }
        });

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().fieldProperty());
        new ValueTableCellFactory<TableColumnsItemModel, Field>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
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
                .withText(field -> ((TableColumnsTabViewModel) viewModel).getFieldDisplayName(field))
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

    private void setupIconButtons() {
        ActionFactory actionFactory = new ActionFactory(preferences.getKeyBindingRepository());

        actionFactory.configureIconButton(StandardActions.COLUMN_SORT_UP, new SimpleCommand() {
            @Override
            public void execute() { ((TableColumnsTabViewModel) viewModel).moveColumnUp(); }
        }, sortColumnUp);

        actionFactory.configureIconButton(StandardActions.COLUMN_SORT_DOWN, new SimpleCommand() {
            @Override
            public void execute() { ((TableColumnsTabViewModel) viewModel).moveColumnDown(); }
        }, sortColumnDown);

        actionFactory.configureIconButton(StandardActions.COLUMN_ADD, new SimpleCommand() {
            @Override
            public void execute() { ((TableColumnsTabViewModel) viewModel).insertColumnInList(); }
        }, addColumn);

        actionFactory.configureIconButton(StandardActions.COLUMNS_UPDATE, new SimpleCommand() {
            @Override
            public void execute() { ((TableColumnsTabViewModel) viewModel).fillColumnList(); }
        }, reloadTableColumns);

        actionFactory.configureIconButton(StandardActions.HELP_SPECIAL_FIELDS, new HelpAction(HelpFile.SPECIAL_FIELDS), specialFieldsHelp);
    }
}
