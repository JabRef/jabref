package org.jabref.gui.keyboard;

import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;

public class KeyBindingsDialogViewModel extends AbstractViewModel {

    private final KeyBindingPreferences keyBindingPreferences;
    private KeyBindingRepository keyBindingRepository;
    private DialogService dialogService;

    public ObjectProperty<KeyBindingViewModel> selectedKeyBindingProperty() {
        return selectedKeyBinding;
    }

    private final ObjectProperty<KeyBindingViewModel> selectedKeyBinding = new SimpleObjectProperty<>();
    private final ObjectProperty<KeyBindingViewModel> rootKeyBinding = new SimpleObjectProperty<>();

    public ObjectProperty<KeyBindingViewModel> rootKeyBindingProperty() {
        return rootKeyBinding;
    }

    public KeyBindingsDialogViewModel(KeyBindingPreferences keyBindingPreferences, DialogService dialogService) {
        this.keyBindingPreferences = Objects.requireNonNull(keyBindingPreferences);
        this.dialogService = Objects.requireNonNull(dialogService);
        keyBindingRepository = new KeyBindingRepository(keyBindingPreferences.getKeyBindings());
        populateTable();
    }

    /**
     * Read all keybindings from the keybinding repository and create table keybinding models for them
     */
    private void populateTable() {
        KeyBindingViewModel root = new KeyBindingViewModel(keyBindingRepository, KeyBindingCategory.FILE);
        for (KeyBindingCategory category : KeyBindingCategory.values()) {
            KeyBindingViewModel categoryItem = new KeyBindingViewModel(keyBindingRepository, category);
            keyBindingRepository.getKeyBindings().forEach((keyBinding, bind) -> {
                if (keyBinding.getCategory() == category) {
                    KeyBindingViewModel keyBindViewModel = new KeyBindingViewModel(keyBindingRepository, keyBinding, bind);
                    categoryItem.getChildren().add(keyBindViewModel);
                }
            });
            root.getChildren().add(categoryItem);
        }
        rootKeyBinding.set(root);
    }

    public void setNewBindingForCurrent(KeyEvent event) {
        // first check if a valid entry is selected
        if (selectedKeyBinding.isNull().get()) {
            return;
        }
        KeyBindingViewModel selectedEntry = selectedKeyBinding.get();
        if ((selectedEntry == null) || (selectedEntry.isCategory())) {
            return;
        }

        if (selectedEntry.setNewBinding(event)) {
            keyBindingRepository.put(selectedEntry.getKeyBinding(), selectedEntry.getBinding());
        }
    }

    public void saveKeyBindings() {
        keyBindingPreferences.setNewKeyBindings(keyBindingRepository.getKeyBindings());

        String title = Localization.lang("Key bindings changed");
        String content = Localization.lang("Your new key bindings have been stored.") + '\n'
                + Localization.lang("You must restart JabRef for the new key bindings to work properly.");
        dialogService.showInformationDialogAndWait(title, content);
    }

    public void resetToDefault() {
        String title = Localization.lang("Resetting all key bindings");
        String content = Localization.lang("All key bindings will be reset to their defaults.");
        ButtonType resetButtonType = new ButtonType("Reset", ButtonBar.ButtonData.OK_DONE);
        dialogService.showCustomButtonDialogAndWait(Alert.AlertType.INFORMATION, title, content, resetButtonType,
                ButtonType.CANCEL).ifPresent(response -> {
            if (response == resetButtonType) {
                keyBindingRepository.resetToDefault();
                populateTable();
            }
        });
    }
}
