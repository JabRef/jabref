package org.jabref.gui.preferences;

import javax.inject.Inject;

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

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.IEEEField;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class TableColumnsTabView extends AbstractPreferenceTabView implements PreferencesTab {

    @FXML private TableView<TableColumnsItemModel> columnsList;
    @FXML private TableColumn<TableColumnsItemModel, Field> nameColumn;
    @FXML private TableColumn<TableColumnsItemModel, Field> actionsColumn;
    @FXML private ComboBox<Field> addText;
    @FXML private Button sortUp;
    @FXML private Button sortDown;
    @FXML private Button addColumn;

    @FXML private Button updateToTable;

    @FXML private CheckBox showFileColumn;

    @FXML private CheckBox showUrlColumn;
    @FXML private RadioButton urlFirst;
    @FXML private RadioButton doiFirst;
    @FXML private CheckBox showEprintColumn;

    @FXML private CheckBox enableSpecialFields;
    @FXML private RadioButton syncKeywords;
    @FXML private RadioButton serializeSpecial;

    @FXML private CheckBox enableExtraColumns;
    @FXML private Button enableSpecialFieldsHelp;

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
        TableColumnsTabViewModel tableColumnsTabViewModel = new TableColumnsTabViewModel(dialogService, preferences, frame);
        this.viewModel = tableColumnsTabViewModel;

        setUpTable();
        setUpBindings();
        setUpButtons();
    }

    private void setUpTable() {
        columnsList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                ((TableColumnsTabViewModel) viewModel).removeColumn(columnsList.getSelectionModel().getSelectedItem());
            }
        });

        nameColumn.setCellValueFactory(cellData -> cellData.getValue().fieldProperty());
        nameColumn.setCellFactory(cellData -> new TableCell<>() {
            @Override
            public void updateItem(Field item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    setText("");
                } else {
                    setText(getFieldName(item));
                }
            }

            private String getString() {
                return getItem() == null ? "" : getItem().getName();
            }
        });
        nameColumn.setSortable(false);
        nameColumn.setReorderable(false);

        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().fieldProperty());
        new ValueTableCellFactory<TableColumnsItemModel, Field>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withOnMouseClickedEvent(item -> {
                    return evt -> {
                        ((TableColumnsTabViewModel) viewModel).removeColumn(columnsList.getFocusModel().getFocusedItem());
                    };
                })
                .install(actionsColumn);

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);

        columnsList.itemsProperty().bind(((TableColumnsTabViewModel) viewModel).columnsListProperty());
        ((TableColumnsTabViewModel) viewModel).selectedColumnModelProperty().setValue(columnsList.getSelectionModel());

        addText.setEditable(true);
        new ViewModelListCellFactory<Field>()
                .withText(this::getFieldName)
                .install(addText);
        addText.itemsProperty().bind(((TableColumnsTabViewModel) viewModel).selectableFieldProperty());
        addText.valueProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).addFieldProperty());
        addText.setConverter(new StringConverter<>() {
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
    }

    private String getFieldName(Field field) {
        if (field instanceof SpecialField) {
            return field.getName() + " (" + Localization.lang("Special") + ")";
        } else if (field instanceof IEEEField) {
            return field.getName() + " (" + Localization.lang("IEEE") + ")";
        } else if (field instanceof InternalField) {
            return field.getName() + " (" + Localization.lang("Internal") + ")";
        } else if (field instanceof UnknownField) {
            return field.getName() + " (" + Localization.lang("Custom") + ")";
        } else if (field instanceof TableColumnsTabViewModel.ExtraFileField) {
            return field.getName() + " (" + Localization.lang("File type") + ")";
        } else {
            return field.getName();
        }
    }

    private void setUpBindings() {
        showFileColumn.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).showFileColumnProperty());
        showUrlColumn.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).showUrlColumnProperty());
        urlFirst.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).preferUrlProperty());
        doiFirst.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).preferDoiProperty());
        showEprintColumn.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).showEPrintColumnProperty());
        enableSpecialFields.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).specialFieldsEnabledProperty());
        syncKeywords.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).specialFieldsSyncKeyWordsProperty());
        serializeSpecial.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).specialFieldsSerializeProperty());
        enableExtraColumns.selectedProperty().bindBidirectional(((TableColumnsTabViewModel) viewModel).showExtraFileColumnsProperty());
    }

    private void setUpButtons() {
        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(PreferencesActions.COLUMN_SORT_UP, new SimpleCommand() {
            @Override
            public void execute() { ((TableColumnsTabViewModel) viewModel).moveColumnUp(); }
        }, sortUp);

        actionFactory.configureIconButton(PreferencesActions.COLUMN_SORT_DOWN, new SimpleCommand() {
            @Override
            public void execute() { ((TableColumnsTabViewModel) viewModel).moveColumnDown(); }
        }, sortDown);

        actionFactory.configureIconButton(PreferencesActions.COLUMN_ADD, new SimpleCommand() {
            @Override
            public void execute() { ((TableColumnsTabViewModel) viewModel).insertColumnInList(); }
        }, addColumn);

        actionFactory.configureIconButton(PreferencesActions.COLUMNS_UPDATE, new SimpleCommand() {
            @Override
            public void execute() { ((TableColumnsTabViewModel) viewModel).fillColumnList(); }
        }, updateToTable);

        actionFactory.configureIconButton(StandardActions.HELP_SPECIAL_FIELDS, new HelpAction(HelpFile.SPECIAL_FIELDS), enableSpecialFieldsHelp);
    }
}
