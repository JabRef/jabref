package org.jabref.gui.preferences;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.KeyEvent;

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

public class TableColumnsTabView extends AbstractPreferenceTabView implements PreferencesTab {

    @FXML private CheckListView<TableColumnsItemModel> columnsList;
    @FXML private Button sortUp;
    @FXML private Button sortDown;
    @FXML private Button addColumn;
    @FXML private Button removeColumn;
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

    private long lastKeyPressTime;
    private String listSearchTerm;

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

        columnsList.itemsProperty().bindBidirectional(tableColumnsTabViewModel.columnsListProperty());
        columnsList.setOnKeyTyped(event -> jumpToSearchKey(columnsList, event));
        columnsList.setCellFactory(checkBoxListView -> new CheckBoxListCell<TableColumnsItemModel>(columnsList::getItemBooleanProperty) {
            @Override
            public void updateItem(TableColumnsItemModel item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    return;
                }

                if (item.getField() instanceof SpecialField) {
                    setText(item.getName() + " (" + Localization.lang("Special") + ")");
                } else if (item.getField() instanceof IEEEField) {
                    setText(item.getName() + " (" + Localization.lang("IEEE") + ")");
                } else if (item.getField() instanceof InternalField) {
                    setText(item.getName() + " (" + Localization.lang("Internal") + ")");
                } else if (item.getField() instanceof UnknownField) {
                    setText(item.getName() + " (" + Localization.lang("Custom") + ")");
                } else if (item.getField() instanceof TableColumnsTabViewModel.ExtraFileField) {
                    setText(item.getName() + " (" + Localization.lang("File Type") + ")");
                } else {
                    setText(item.getName());
                }
            }
        });

        tableColumnsTabViewModel.checkedColumnsModelProperty().setValue(columnsList.getCheckModel());

        showFileColumn.selectedProperty().bindBidirectional(tableColumnsTabViewModel.showFileColumnProperty());
        showUrlColumn.selectedProperty().bindBidirectional(tableColumnsTabViewModel.showUrlColumnProperty());
        urlFirst.selectedProperty().bindBidirectional(tableColumnsTabViewModel.preferUrlProperty());
        doiFirst.selectedProperty().bindBidirectional(tableColumnsTabViewModel.preferDoiProperty());
        showEprintColumn.selectedProperty().bindBidirectional(tableColumnsTabViewModel.showEPrintColumnProperty());
        enableSpecialFields.selectedProperty().bindBidirectional(tableColumnsTabViewModel.specialFieldsEnabledProperty());
        syncKeywords.selectedProperty().bindBidirectional(tableColumnsTabViewModel.specialFieldsSyncKeyWordsProperty());
        serializeSpecial.selectedProperty().bindBidirectional(tableColumnsTabViewModel.specialFieldsSerializeProperty());
        enableExtraColumns.selectedProperty().bindBidirectional(tableColumnsTabViewModel.showExtraFileColumnsProperty());

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

    private void jumpToSearchKey(CheckListView<TableColumnsItemModel> list, KeyEvent keypressed) {
        if (keypressed.getCharacter() == null) {
            return;
        }

        if (System.currentTimeMillis() - lastKeyPressTime < 1000) {
            listSearchTerm += keypressed.getCharacter().toLowerCase();
        } else {
            listSearchTerm = keypressed.getCharacter().toLowerCase();
        }

        lastKeyPressTime = System.currentTimeMillis();

        list.getItems().stream().filter(item -> item.getName().toLowerCase().startsWith(listSearchTerm))
                .findFirst().ifPresent(list::scrollTo);
    }
}
