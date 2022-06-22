package org.jabref.gui.preferences.keybindings;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.preferences.keybindings.presets.KeyBindingPreset;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.ViewModelTreeTableCellFactory;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class KeyBindingsTab extends AbstractPreferenceTabView<KeyBindingsTabViewModel> implements PreferencesTab {

    @FXML private TreeTableView<KeyBindingViewModel> keyBindingsTable;
    @FXML private TreeTableColumn<KeyBindingViewModel, String> actionColumn;
    @FXML private TreeTableColumn<KeyBindingViewModel, String> shortcutColumn;
    @FXML private TreeTableColumn<KeyBindingViewModel, KeyBindingViewModel> resetColumn;
    @FXML private TreeTableColumn<KeyBindingViewModel, KeyBindingViewModel> clearColumn;
    @FXML private MenuButton presetsButton;

    @Inject private KeyBindingRepository keyBindingRepository;

    public KeyBindingsTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Key bindings");
    }

    @FXML
    private void initialize() {
        viewModel = new KeyBindingsTabViewModel(keyBindingRepository, dialogService, preferencesService);

        keyBindingsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        viewModel.selectedKeyBindingProperty().bind(
                EasyBind.wrapNullable(keyBindingsTable.selectionModelProperty())
                        .mapObservable(SelectionModel::selectedItemProperty)
                        .mapObservable(TreeItem::valueProperty)
        );
        keyBindingsTable.setOnKeyPressed(evt -> viewModel.setNewBindingForCurrent(evt));
        keyBindingsTable.rootProperty().bind(
                EasyBind.map(viewModel.rootKeyBindingProperty(),
                        keybinding -> new RecursiveTreeItem<>(keybinding, KeyBindingViewModel::getChildren))
        );
        actionColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().nameProperty());
        shortcutColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().shownBindingProperty());
        new ViewModelTreeTableCellFactory<KeyBindingViewModel>()
                .withGraphic(keyBinding -> keyBinding.getResetIcon().map(JabRefIcon::getGraphicNode).orElse(null))
                .withOnMouseClickedEvent(keyBinding -> evt -> keyBinding.resetToDefault())
                .install(resetColumn);
        new ViewModelTreeTableCellFactory<KeyBindingViewModel>()
                .withGraphic(keyBinding -> keyBinding.getClearIcon().map(JabRefIcon::getGraphicNode).orElse(null))
                .withOnMouseClickedEvent(keyBinding -> evt -> keyBinding.clear())
                .install(clearColumn);

        viewModel.keyBindingPresets().forEach(preset -> presetsButton.getItems().add(createMenuItem(preset)));
    }

    private MenuItem createMenuItem(KeyBindingPreset preset) {
        MenuItem item = new MenuItem(preset.getName());
        item.setOnAction((event) -> viewModel.loadPreset(preset));
        return item;
    }

    @FXML
    private void resetBindings() {
        viewModel.resetToDefault();
    }
}
