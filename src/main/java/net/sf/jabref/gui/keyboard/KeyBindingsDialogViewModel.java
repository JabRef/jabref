/*  Copyright (C) 2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.keyboard;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;

import net.sf.jabref.gui.FXDialogs;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

public class KeyBindingsDialogViewModel {

    private KeyBindingRepository keyBindingRepository;
    private KeyBindingPreferences keyBindingPreferences;
    private final ObjectProperty<TreeItem<KeyBindingViewModel>> selectedKeyBinding = new SimpleObjectProperty<>();


    @FXML
    private TreeTableView<KeyBindingViewModel> keyBindingsTable;

    @FXML
    private TreeTableColumn<KeyBindingViewModel, String> actionColumn;

    @FXML
    private TreeTableColumn<KeyBindingViewModel, String> shortcutColumn;

    @FXML
    private TreeTableColumn<KeyBindingViewModel, String> resetColumn;

    @FXML
    private Button closeButton;

    @FXML
    private Button resetButton;


    @FXML
    private void initialize() {
        keyBindingsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        ButtonBar.setButtonData(resetButton, ButtonData.LEFT);
        selectedKeyBinding.bind(keyBindingsTable.getSelectionModel().selectedItemProperty());
    }

    public void initializeView() {
        registerKeyEvents();
        populateTable();
        bindColumnValues();
    }

    /**
     * Read all keybindings from the keybinding repository and create table keybinding
     * models for them
     */
    private void populateTable() {
        TreeItem<KeyBindingViewModel> root = new TreeItem<>(new KeyBindingViewModel(KeyBindingCategory.FILE));
        for (KeyBindingCategory category : KeyBindingCategory.values()) {
            TreeItem<KeyBindingViewModel> categoryItem = new TreeItem<>(new KeyBindingViewModel(category));
            keyBindingRepository.getKeyBindings().forEach((keyBinding, bind) -> {
                if (keyBinding.getCategory() == category) {
                    KeyBindingViewModel keyBindViewModel = new KeyBindingViewModel(keyBinding, bind);
                    TreeItem<KeyBindingViewModel> keyBindTreeItem = new TreeItem<>(keyBindViewModel);
                    categoryItem.getChildren().add(keyBindTreeItem);
                }
            });
            categoryItem.setExpanded(true);
            root.getChildren().add(categoryItem);
        }
        root.setExpanded(true);
        keyBindingsTable.setRoot(root);
    }

    private void bindColumnValues() {
        actionColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().nameProperty());
        shortcutColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().shownBindingProperty());
        resetColumn.setCellFactory(
                new Callback<TreeTableColumn<KeyBindingViewModel, String>, TreeTableCell<KeyBindingViewModel, String>>() {

                    @Override
                    public TreeTableCell<KeyBindingViewModel, String> call(
                            TreeTableColumn<KeyBindingViewModel, String> p) {
                        return new TreeTableCell<KeyBindingViewModel, String>() {

                            @Override
                            public void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                KeyBindingViewModel viewModel = getTreeTableRow().getItem();
                                if ((viewModel != null) && !viewModel.isCategory()) {
                                    Text graphic = new Text(IconTheme.JabRefIcon.CLEANUP_ENTRIES.getCode());
                                    graphic.getStyleClass().add("icon");
                                    setGraphic(graphic);
                                    setOnMouseClicked(evt -> {
                                        getTreeTableRow().getItem().resetToDefault(keyBindingRepository);
                                    });

                                }
                                keyBindingsTable.refresh();
                            }
                        };
                    }
                });
    }

    private void registerKeyEvents() {
        keyBindingsTable.setOnKeyPressed(evt -> grabKey(evt));
    }

    private Stage getDialogStage() {
        return (Stage) closeButton.getScene().getWindow();
    }

    @FXML
    private void closeDialog() {
        Stage stage = getDialogStage();
        stage.close();
    }

    public void saveKeyBindings() {
        keyBindingPreferences.setNewKeyBindings(keyBindingRepository.getKeyBindings());
    }

    @FXML
    private void saveKeyBindingsAndCloseDialog() {
        saveKeyBindings();

        String title = Localization.lang("Key bindings changed");
        String content = Localization.lang("Your new key bindings have been stored.") + '\n'
                + Localization.lang("You must restart JabRef for the new key bindings to work properly.");
        FXDialogs.showInformationDialogAndWait(title, content);
        closeDialog();
    }

    @FXML
    private void setDefaultBindings() {
        String title = Localization.lang("Resetting all key bindings");
        String content = Localization.lang("All key bindings will be reset to their defaults.");
        ButtonType resetButtonType = new ButtonType("Reset", ButtonData.OK_DONE);
        FXDialogs.showCustomButtonDialogAndWait(AlertType.INFORMATION, title, content, resetButtonType,
                ButtonType.CANCEL).ifPresent(response -> {
                    if (response == resetButtonType) {
                        resetKeyBindingsToDefault();
                        populateTable();
                    }
                });
    }

    public void resetKeyBindingsToDefault() {
        keyBindingRepository.resetToDefault();
    }

    public void grabKey(KeyEvent evt) {
        // first check if a valid entry is selected
        if (selectedKeyBinding.isNull().get()) {
            return;
        }
        KeyBindingViewModel selectedEntry = selectedKeyBinding.get().getValue();
        if ((selectedEntry == null) || (selectedEntry.isCategory())) {
            return;
        }

        if (selectedEntry.setNewBinding(evt)) {
            keyBindingRepository.put(selectedEntry.getKeyBinding(), selectedEntry.getBinding());
        }
    }

    public void setKeyBindingPreferences(KeyBindingPreferences keyBindingPreferences) {
        this.keyBindingPreferences = keyBindingPreferences;
        this.keyBindingRepository = new KeyBindingRepository(keyBindingPreferences.getKeyBindings());
    }

    public ObjectProperty<TreeItem<KeyBindingViewModel>> getSelectedKeyBinding() {
        return selectedKeyBinding;
    }

    public KeyBindingRepository getKeyBindingRepository() {
        return keyBindingRepository;
    }

}
