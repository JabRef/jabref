package org.jabref.gui.preferences.keybindings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingCategory;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.preferences.keybindings.presets.BashKeyBindingPreset;
import org.jabref.gui.preferences.keybindings.presets.KeyBindingPreset;
import org.jabref.gui.preferences.keybindings.presets.NewEntryBindingPreset;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.l10n.Localization;

public class KeyBindingsTabViewModel implements PreferenceTabViewModel {

    private final KeyBindingRepository keyBindingRepository;
    private final GuiPreferences preferences;
    private final OptionalObjectProperty<KeyBindingViewModel> selectedKeyBinding = OptionalObjectProperty.empty();
    private final ObjectProperty<KeyBindingViewModel> rootKeyBinding = new SimpleObjectProperty<>();
    private final ListProperty<KeyBindingPreset> keyBindingPresets = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final DialogService dialogService;

    private final List<String> restartWarning = new ArrayList<>();

    public KeyBindingsTabViewModel(KeyBindingRepository keyBindingRepository, DialogService dialogService, GuiPreferences preferences) {
        this.keyBindingRepository = new KeyBindingRepository(keyBindingRepository.getKeyBindings());
        this.dialogService = Objects.requireNonNull(dialogService);
        this.preferences = Objects.requireNonNull(preferences);

        keyBindingPresets.add(new BashKeyBindingPreset());
        keyBindingPresets.add(new NewEntryBindingPreset());
    }

    /**
     * Read all keybindings from the keybinding repository and create table keybinding models for them
     */
    @Override
    public void setValues() {
        KeyBindingViewModel root = new KeyBindingViewModel(keyBindingRepository, KeyBindingCategory.FILE);
        rootKeyBinding.set(root);
        filterValues("");
    }

    public void filterValues(String term) {
        KeyBindingViewModel root = rootKeyBinding.get();
        root.clear();
        root.getChildren().clear();
        for (KeyBindingCategory category : KeyBindingCategory.values()) {
            KeyBindingViewModel categoryItem = new KeyBindingViewModel(keyBindingRepository, category);
            keyBindingRepository.getKeyBindings().forEach((keyBinding, bind) -> {
                if (keyBinding.getCategory() != category) {
                    return;
                }
                if (matchesSearchTerm(keyBinding, term.toLowerCase())) {
                    KeyBindingViewModel keyBindViewModel = new KeyBindingViewModel(keyBindingRepository, keyBinding, bind);
                    categoryItem.getChildren().add(keyBindViewModel);
                }
            });
            if (!categoryItem.getChildren().isEmpty()) {
                root.getChildren().add(categoryItem);
            }
        }
    }

    /**
     * Searches for the term in the keybinding's localization, category, or key combination
     *
     * @param keyBinding keybinding to search in
     * @param searchTerm      term to search for
     * @return true if the term is found in the keybinding
     */
    private boolean matchesSearchTerm(KeyBinding keyBinding, String searchTerm) {
        if (keyBinding.getLocalization().toLowerCase().contains(searchTerm) ||
                keyBinding.getCategory().getName().toLowerCase().contains(searchTerm)) {
            return true;
        }
        if (keyBindingRepository.getKeyCombination(keyBinding).isPresent()) {
            return keyBindingRepository.getKeyCombination(keyBinding).get().toString().toLowerCase().contains(searchTerm);
        }
        return false;
    }

    public void setNewBindingForCurrent(KeyEvent event) {
        Optional<KeyBindingViewModel> selectedKeyBindingValue = selectedKeyBinding.getValue();
        if (selectedKeyBindingValue.isEmpty()) {
            return;
        }

        KeyBindingViewModel selectedEntry = selectedKeyBindingValue.get();
        if (selectedEntry.isCategory()) {
            return;
        }

        if (selectedEntry.setNewBinding(event)) {
            keyBindingRepository.put(selectedEntry.getKeyBinding(), selectedEntry.getBinding());
        }
    }

    public void storeSettings() {
        if (!keyBindingRepository.equals(preferences.getKeyBindingRepository())) {
            preferences.getKeyBindingRepository().getBindingsProperty().set(keyBindingRepository.getBindingsProperty());
            restartWarning.add(Localization.lang("Keyboard shortcuts changed"));
        }
    }

    public void resetToDefault() {
        String title = Localization.lang("Resetting all keyboard shortcuts");
        String content = Localization.lang("All keyboard shortcuts will be reset to their defaults.");
        ButtonType resetButtonType = new ButtonType("Reset", ButtonBar.ButtonData.OK_DONE);
        dialogService.showCustomButtonDialogAndWait(Alert.AlertType.INFORMATION, title, content, resetButtonType,
                ButtonType.CANCEL).ifPresent(response -> {
            if (response == resetButtonType) {
                keyBindingRepository.resetToDefault();
                setValues();
            }
        });
    }

    public void loadPreset(KeyBindingPreset preset) {
        if (preset == null) {
            return;
        }

        preset.getKeyBindings().forEach(keyBindingRepository::put);
        setValues();
    }

    public ListProperty<KeyBindingPreset> keyBindingPresets() {
        return keyBindingPresets;
    }

    @Override
    public List<String> getRestartWarnings() {
        return restartWarning;
    }

    public OptionalObjectProperty<KeyBindingViewModel> selectedKeyBindingProperty() {
        return selectedKeyBinding;
    }

    public ObjectProperty<KeyBindingViewModel> rootKeyBindingProperty() {
        return rootKeyBinding;
    }

    public KeyBindingRepository getKeyBindingRepository() {
        return keyBindingRepository;
    }
}
