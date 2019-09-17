package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;

import org.jabref.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class NameFormatterTabView extends AbstractPreferenceTabView<NameFormatterTabViewModel> implements PreferencesTab {

    @FXML private TableView<NameFormatterItemModel> formatterList;
    @FXML private TableColumn<NameFormatterItemModel, String> formatterNameColumn;
    @FXML private TableColumn<NameFormatterItemModel, String> formatterStringColumn;
    @FXML private TableColumn<NameFormatterItemModel, String> actionsColumn;
    @FXML private TextField addFormatterName;
    @FXML private TextField addFormatterString;
    @FXML private Button formatterHelp;

    public NameFormatterTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @Override
    public String getTabName() { return Localization.lang("Name formatter"); }

    public void initialize () {
        this.viewModel = new NameFormatterTabViewModel(dialogService, preferences);

        formatterList.setEditable(true);

        formatterNameColumn.setSortable(true);
        formatterNameColumn.setReorderable(false);
        formatterNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        formatterNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        formatterNameColumn.setEditable(true);
        formatterNameColumn.setOnEditCommit(
                (TableColumn.CellEditEvent<NameFormatterItemModel, String> event) ->
                        event.getRowValue().setName(event.getNewValue()));

        formatterStringColumn.setSortable(true);
        formatterStringColumn.setReorderable(false);
        formatterStringColumn.setCellValueFactory(cellData -> cellData.getValue().formatProperty());
        formatterStringColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        formatterStringColumn.setEditable(true);
        formatterStringColumn.setOnEditCommit(
                (TableColumn.CellEditEvent<NameFormatterItemModel, String> event) ->
                    event.getRowValue().setFormat(event.getNewValue()));

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<NameFormatterItemModel, String>()
                .withGraphic(name -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove") + " " + name)
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.removeFormatter(formatterList.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        formatterList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                viewModel.removeFormatter(formatterList.getSelectionModel().getSelectedItem());
            }
        });

        formatterList.itemsProperty().bindBidirectional(viewModel.formatterListProperty());

        addFormatterName.textProperty().bindBidirectional(viewModel.addFormatterNameProperty());
        addFormatterString.textProperty().bindBidirectional(viewModel.addFormatterStringProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP_NAME_FORMATTER, new HelpAction(HelpFile.CUSTOM_EXPORTS_NAME_FORMATTER), formatterHelp);
    }

    public void addFormatter() { viewModel.addFormatter(); }

}
