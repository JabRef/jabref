package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

import org.jabref.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class XmpPrivacyTabView extends AbstractPreferenceTabView implements PreferencesTab {

    @FXML private CheckBox enableXmpFilter;
    @FXML private TableView<XmpPrivacyItemModel> filterList;
    @FXML private TableColumn<XmpPrivacyItemModel, Field> fieldColumn;
    @FXML private TableColumn<XmpPrivacyItemModel, Field> actionsColumn;
    @FXML private ComboBox<Field> addFieldName;
    @FXML private Button addField;

    public XmpPrivacyTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    public void initialize () {
        XmpPrivacyTabViewModel xmpPrivacyTabViewModel = new XmpPrivacyTabViewModel(dialogService, preferences);
        this.viewModel = xmpPrivacyTabViewModel;

        enableXmpFilter.selectedProperty().bindBidirectional(xmpPrivacyTabViewModel.xmpFilterEnabledProperty());
        filterList.disableProperty().bind(xmpPrivacyTabViewModel.xmpFilterEnabledProperty().not());
        addFieldName.disableProperty().bind(xmpPrivacyTabViewModel.xmpFilterEnabledProperty().not());
        addField.disableProperty().bind(xmpPrivacyTabViewModel.xmpFilterEnabledProperty().not());

        fieldColumn.setSortable(true);
        fieldColumn.setReorderable(false);
        fieldColumn.setCellValueFactory(cellData -> cellData.getValue().fieldProperty());
        fieldColumn.setCellFactory(cellData -> new TableCell<>() {
            @Override
            public void updateItem(Field item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    setText("");
                } else {
                    setText(xmpPrivacyTabViewModel.getFieldDisplayName(item));
                }
            }

            private String getString() {
                return getItem() == null ? "" : getItem().getName();
            }
        });

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().fieldProperty());
        new ValueTableCellFactory<XmpPrivacyItemModel, Field>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(item -> Localization.lang("Remove") + " " + item.getName())
                .withOnMouseClickedEvent(item -> evt -> {
                    xmpPrivacyTabViewModel.removeFilter(filterList.getFocusModel().getFocusedItem());
                })
                .install(actionsColumn);

        filterList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                xmpPrivacyTabViewModel.removeFilter(filterList.getSelectionModel().getSelectedItem());
            }
        });

        filterList.itemsProperty().bind(xmpPrivacyTabViewModel.filterListProperty());

        addFieldName.setEditable(true);
        new ViewModelListCellFactory<Field>()
                .withText(xmpPrivacyTabViewModel::getFieldDisplayName)
                .install(addFieldName);
        addFieldName.itemsProperty().bind(xmpPrivacyTabViewModel.availableFieldsProperty());
        addFieldName.valueProperty().bindBidirectional(xmpPrivacyTabViewModel.addFieldNameProperty());
        addFieldName.setConverter(new StringConverter<>() {
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
        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.XMP_FILTER_ADD, new SimpleCommand() {
            @Override
            public void execute() { xmpPrivacyTabViewModel.addField(); }
        }, addField);
    }

    @Override
    public String getTabName() { return Localization.lang("XMP-metadata"); }
}
