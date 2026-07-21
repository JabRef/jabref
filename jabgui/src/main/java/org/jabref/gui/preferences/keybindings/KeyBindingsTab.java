package org.jabref.gui.preferences.keybindings;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.keybindings.presets.KeyBindingPreset;
import org.jabref.gui.util.ColorUtil;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.ViewModelTreeTableCellFactory;
import org.jabref.logic.l10n.Localization;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.textfield.CustomTextField;

public class KeyBindingsTab extends AbstractPreferenceTabView<KeyBindingsTabViewModel> {

    private final TreeTableView<KeyBindingViewModel> keyBindingsTable = new TreeTableView<>();

    public KeyBindingsTab() {
        viewModel = new KeyBindingsTabViewModel(preferences.getKeyBindingRepository(), dialogService, preferences);
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Keyboard shortcuts");
    }

    private void buildView() {
        getChildren().add(form()
                .title(Localization.lang("Keyboard shortcuts"))
                .custom(buildBindingsEditor())
                .custom(buildButtonRow())
                .build());
    }

    private Node buildBindingsEditor() {
        CustomTextField searchBox = new CustomTextField();
        searchBox.setPromptText(Localization.lang("Filter"));
        searchBox.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());
        VBox.setMargin(searchBox, new Insets(3.0));
        searchBox.textProperty().addListener((_, previousText, searchTerm) -> {
            viewModel.filterValues(searchTerm);
            setCategoriesExpanded(!searchTerm.isEmpty() || previousText.isEmpty());
        });

        ObjectProperty<Color> flashingColor = new SimpleObjectProperty<>(Color.TRANSPARENT);
        StringProperty flashingColorStringProperty = ColorUtil.createFlashingColorStringProperty(flashingColor);
        searchBox.styleProperty().bind(
                new SimpleStringProperty("-fx-control-inner-background: ").concat(flashingColorStringProperty).concat(";")
        );

        setUpTable();

        return new VBox(10.0, searchBox, keyBindingsTable);
    }

    private void setUpTable() {
        keyBindingsTable.setShowRoot(false);
        keyBindingsTable.getStyleClass().add("keybinding-table");
        keyBindingsTable.setPadding(new Insets(0, 10.0, 0, 0));
        keyBindingsTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

        TreeTableColumn<KeyBindingViewModel, String> actionColumn = new TreeTableColumn<>(Localization.lang("Action"));
        actionColumn.setPrefWidth(200.0);
        actionColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().nameProperty());

        TreeTableColumn<KeyBindingViewModel, String> shortcutColumn = new TreeTableColumn<>(Localization.lang("Shortcut"));
        shortcutColumn.setPrefWidth(100.0);
        shortcutColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().shownBindingProperty());

        TreeTableColumn<KeyBindingViewModel, KeyBindingViewModel> resetColumn = new TreeTableColumn<>();
        resetColumn.setMinWidth(30.0);
        resetColumn.setMaxWidth(30.0);
        resetColumn.setPrefWidth(30.0);
        new ViewModelTreeTableCellFactory<KeyBindingViewModel>()
                .withGraphic(keyBinding -> keyBinding.getResetIcon().map(JabRefIcon::getGraphicNode).orElse(null))
                .withOnMouseClickedEvent(keyBinding -> _ -> keyBinding.resetToDefault())
                .withStyleClass(_ -> "keybinding-table-icon-cell")
                .install(resetColumn);

        TreeTableColumn<KeyBindingViewModel, KeyBindingViewModel> clearColumn = new TreeTableColumn<>();
        clearColumn.setMinWidth(30.0);
        clearColumn.setMaxWidth(30.0);
        clearColumn.setPrefWidth(30.0);
        new ViewModelTreeTableCellFactory<KeyBindingViewModel>()
                .withGraphic(keyBinding -> keyBinding.getClearIcon().map(JabRefIcon::getGraphicNode).orElse(null))
                .withOnMouseClickedEvent(keyBinding -> _ -> keyBinding.clear())
                .withStyleClass(_ -> "keybinding-table-icon-cell")
                .install(clearColumn);

        keyBindingsTable.getColumns().add(actionColumn);
        keyBindingsTable.getColumns().add(shortcutColumn);
        keyBindingsTable.getColumns().add(resetColumn);
        keyBindingsTable.getColumns().add(clearColumn);

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
    }

    private Node buildButtonRow() {
        Button expandAll = new Button(Localization.lang("Expand all"));
        expandAll.setOnAction(_ -> setCategoriesExpanded(true));
        Button collapseAll = new Button(Localization.lang("Collapse all"));
        collapseAll.setOnAction(_ -> setCategoriesExpanded(false));
        HBox left = new HBox(10.0, expandAll, collapseAll);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        MenuButton presetsButton = new MenuButton(Localization.lang("Presets"));
        presetsButton.getStyleClass().add("button");
        viewModel.keyBindingPresets().forEach(preset -> presetsButton.getItems().add(createMenuItem(preset)));

        Button resetAll = new Button(Localization.lang("Reset all"));
        resetAll.setGraphic(IconTheme.JabRefIcons.REFRESH.getGraphicNode());
        resetAll.setOnAction(_ -> viewModel.resetToDefault());

        HBox right = new HBox(10.0, presetsButton, resetAll);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(right, Priority.ALWAYS);

        return new HBox(left, right);
    }

    private MenuItem createMenuItem(KeyBindingPreset preset) {
        MenuItem item = new MenuItem(preset.getName());
        item.setOnAction(_ -> viewModel.loadPreset(preset));
        return item;
    }

    private void setCategoriesExpanded(boolean expanded) {
        for (TreeItem<KeyBindingViewModel> child : keyBindingsTable.getRoot().getChildren()) {
            child.setExpanded(expanded);
        }
    }
}
