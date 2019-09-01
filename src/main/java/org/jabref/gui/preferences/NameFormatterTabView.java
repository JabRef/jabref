package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;

import org.jabref.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class NameFormatterTabView extends AbstractPreferenceTabView implements PreferencesTab {

    @FXML private TableView<NameFormatterItemModel> formatterList;
    @FXML private TableColumn<NameFormatterItemModel, String> formatterNameColumn;
    @FXML private TableColumn<NameFormatterItemModel, String> formatterStringColumn;
    @FXML private TableColumn<NameFormatterItemModel, String> actionsColumn;
    @FXML private TextField addFormatterName;
    @FXML private TextField addFormatterString;
    @FXML private Button formatterHelp;
    @FXML private Button addFormatter;

    public NameFormatterTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    public void initialize () {
        NameFormatterTabViewModel nameFormatterTabViewModel = new NameFormatterTabViewModel(dialogService, preferences);
        this.viewModel = nameFormatterTabViewModel;

        formatterList.setEditable(true);

        formatterNameColumn.setSortable(true);
        formatterNameColumn.setReorderable(false);
        formatterNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        formatterNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        formatterNameColumn.setOnEditCommit(
                (TableColumn.CellEditEvent<NameFormatterItemModel, String> event) -> {
                    event.getTableView().getItems().get(
                            event.getTablePosition().getRow())
                            .setName(event.getNewValue());
                });

        formatterStringColumn.setSortable(true);
        formatterStringColumn.setReorderable(false);
        formatterStringColumn.setCellValueFactory(new PropertyValueFactory<>("format"));
        formatterStringColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        formatterStringColumn.setOnEditCommit(
                (TableColumn.CellEditEvent<NameFormatterItemModel, String> event) -> {
                    event.getTableView().getItems().get(
                            event.getTablePosition().getRow())
                            .setFormat(event.getNewValue());
                });

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        new ValueTableCellFactory<NameFormatterItemModel, String>()
                .withGraphic(name -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove") + " " + name)
                .withOnMouseClickedEvent(item -> evt -> {
                    nameFormatterTabViewModel.removeFormatter(formatterList.getFocusModel().getFocusedItem());
                })
                .install(actionsColumn);

        formatterList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                nameFormatterTabViewModel.removeFormatter(formatterList.getSelectionModel().getSelectedItem());
            }
        });

        formatterList.itemsProperty().bindBidirectional(nameFormatterTabViewModel.formatterListProperty());

        addFormatterName.textProperty().bindBidirectional(nameFormatterTabViewModel.addFormatterNameProperty());
        addFormatterString.textProperty().bindBidirectional(nameFormatterTabViewModel.addFormatterStringProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP_NAME_FORMATTER, new HelpAction(HelpFile.CUSTOM_EXPORTS_NAME_FORMATTER), formatterHelp);
        actionFactory.configureIconButton(StandardActions.NAME_FORMATTER_ADD, new SimpleCommand() {
            @Override
            public void execute() { nameFormatterTabViewModel.addFormatter(); }
        }, addFormatter);
    }

    @Override
    public String getTabName() { return Localization.lang("Name formatter"); }
}
