package org.jabref.gui.preferences.keybindings;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.preferences.keybindings.presets.KeyBindingPreset;
import org.jabref.gui.util.ColorUtil;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.ViewModelTreeTableCellFactory;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;
import org.controlsfx.control.textfield.CustomTextField;

public class KeyBindingsTab extends AbstractPreferenceTabView<KeyBindingsTabViewModel> implements PreferencesTab {

    @FXML private CustomTextField searchBox;
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
        return Localization.lang("Keyboard shortcuts");
    }

    @FXML
    private void initialize() {
        viewModel = new KeyBindingsTabViewModel(keyBindingRepository, dialogService, preferences);

        keyBindingsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        viewModel.selectedKeyBindingProperty().bind(
                EasyBind.wrapNullable(keyBindingsTable.selectionModelProperty())
                        .mapObservable(SelectionModel::selectedItemProperty)
                        .mapObservable(TreeItem::valueProperty)
        );
        keyBindingsTable.setOnKeyPressed(viewModel::setNewBindingForCurrent);

        keyBindingsTable.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            event.consume();
            viewModel.setNewBindingForCurrent(event);
        });

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

        searchBox.textProperty().addListener((observable, previousText, searchTerm) -> {
            viewModel.filterValues(searchTerm);
            setCategoriesExpanded(!searchTerm.isEmpty() || previousText.isEmpty());
        });

        ObjectProperty<Color> flashingColor = new SimpleObjectProperty<>(Color.TRANSPARENT);
        StringProperty flashingColorStringProperty = ColorUtil.createFlashingColorStringProperty(flashingColor);

        searchBox.styleProperty().bind(
                new SimpleStringProperty("-fx-control-inner-background: ").concat(flashingColorStringProperty).concat(";")
        );

        searchBox.setPromptText(Localization.lang("Search..."));
        searchBox.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());
    }

    private MenuItem createMenuItem(KeyBindingPreset preset) {
        MenuItem item = new MenuItem(preset.getName());
        item.setOnAction(event -> viewModel.loadPreset(preset));
        return item;
    }

    @FXML
    private void resetBindings() {
        viewModel.resetToDefault();
    }

    @FXML
    private void expandAll() {
        setCategoriesExpanded(true);
    }

    @FXML
    private void collapseAll() {
        setCategoriesExpanded(false);
    }

    private void setCategoriesExpanded(boolean expanded) {
        for (TreeItem<KeyBindingViewModel> child : keyBindingsTable.getRoot().getChildren()) {
            child.setExpanded(expanded);
        }
    }
}
