package org.jabref.gui.preferences;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.VBox;

import org.fxmisc.easybind.EasyBind;
import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.IEEEField;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import org.controlsfx.control.CheckListView;

public class TableColumnsTabView extends VBox implements PrefsTab {



    @FXML private CheckListView<TableColumnsNodeViewModel> columnsList;
    @FXML public Button sortUp;
    @FXML public Button sortDown;
    @FXML public Button addColumn;
    @FXML public Button removeColumn;
    @FXML public Button updateToTable;

    @FXML public CheckBox enableSpecialFields;
    @FXML public RadioButton syncKeywords;
    @FXML public RadioButton writeSpecial;

    @FXML public CheckBox enableIdentifierFields;
    public RadioButton urlFirst;
    public RadioButton doiFirst;

    @FXML public CheckBox enableExtraColumns;
    @FXML private Button enableSpecialFieldsHelp;

    @Inject private DialogService dialogService;
    private final JabRefPreferences preferences;
    private final JabRefFrame frame;

    private TableColumnsTabViewModel viewModel;

    public TableColumnsTabView(JabRefPreferences preferences, JabRefFrame frame) {
        this.preferences = preferences;
        this.frame = frame;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    public void initialize() {
        viewModel = new TableColumnsTabViewModel(dialogService, preferences, frame);

        columnsList.itemsProperty().bindBidirectional(viewModel.columnsNamesProperty());
        columnsList.setCellFactory(checkBoxListView -> new CheckBoxListCell<TableColumnsNodeViewModel>(columnsList::getItemBooleanProperty) {
            @Override
            public void updateItem(TableColumnsNodeViewModel column, boolean empty) {
                super.updateItem(column, empty);
                if (column == null) {
                    return;
                }

                if (column.getField() instanceof SpecialField) {
                    setText(column.getName() + " (" + Localization.lang("Special") + ")");
                } else if (column.getField() instanceof IEEEField) {
                    setText(column.getName() + " (" + Localization.lang("IEEE") + ")");
                } else if (column.getField() instanceof InternalField) {
                    setText(column.getName() + " (" + Localization.lang("Internal") + ")");
                } else if (column.getField() instanceof UnknownField) {
                    setText(column.getName() + " (" + Localization.lang("Custom") + ")");
                } else {
                    setText(column.getName());
                }
            }
        });
        EasyBind.listBind(viewModel.getCheckedColumns(), columnsList.getCheckModel().getCheckedItems());

        enableSpecialFields.selectedProperty().bindBidirectional(viewModel.specialFieldsEnabledProperty());
        enableIdentifierFields.selectedProperty().bindBidirectional(viewModel.identifierFieldsEnabledProperty());
        enableExtraColumns.selectedProperty().bindBidirectional(viewModel.extraFieldsEnabledProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(PreferencesActions.COLUMN_SORT_UP, new SimpleCommand() {
            @Override
            public void execute() { String ab = "a" + "b"; }
        }, sortUp);
        actionFactory.configureIconButton(PreferencesActions.COLUMN_SORT_DOWN, new SimpleCommand() {
            @Override
            public void execute() { String ab = "a" + "b"; }
        }, sortDown);
        actionFactory.configureIconButton(PreferencesActions.COLUMN_ADD, new SimpleCommand() {
            @Override
            public void execute() { String ab = "a" + "b"; }
        }, addColumn);
        actionFactory.configureIconButton(PreferencesActions.COLUMN_REMOVE, new SimpleCommand() {
            @Override
            public void execute() { String ab = "a" + "b"; }
        }, removeColumn);
        actionFactory.configureIconButton(PreferencesActions.COLUMNS_UPDATE, new SimpleCommand() {
            @Override
            public void execute() { String ab = "a" + "b"; }
        }, updateToTable);
        actionFactory.configureIconButton(StandardActions.HELP_SPECIAL_FIELDS, new HelpAction(HelpFile.SPECIAL_FIELDS), enableSpecialFieldsHelp);
    }

    @Override
    public Node getBuilder() {
        return this;
    }

    @Override
    public void setValues() { viewModel.setValues(); }

    @Override
    public void storeSettings() { viewModel.storeSettings(); }

    @Override
    public boolean validateSettings() { return viewModel.validateSettings(); }

    @Override
    public String getTabName() {
        return Localization.lang("Entry table columns");
    }
}
