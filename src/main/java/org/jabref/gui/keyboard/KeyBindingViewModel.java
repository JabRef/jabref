package org.jabref.gui.keyboard;

import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.IconTheme;

import com.google.common.base.CaseFormat;

/**
 * This class represents a view model for objects of the KeyBinding
 * class. It has two properties representing the localized name of an
 * action and its key bind. It can also represent a key binding category
 * instead of a key bind itself.
 *
 */
public class KeyBindingViewModel {

    private KeyBinding keyBinding = null;
    private String realBinding = "";
    private final ObservableList<KeyBindingViewModel> children = FXCollections.observableArrayList();
    private final KeyBindingRepository keyBindingRepository;
    private final SimpleStringProperty displayName = new SimpleStringProperty();
    private final SimpleStringProperty shownBinding = new SimpleStringProperty();

    private final KeyBindingCategory category;

    public KeyBindingViewModel(KeyBindingRepository keyBindingRepository, KeyBinding keyBinding, String binding) {
        this(keyBindingRepository, keyBinding.getCategory());
        this.keyBinding = keyBinding;
        setDisplayName();
        setBinding(binding);
    }

    public KeyBindingViewModel(KeyBindingRepository keyBindingRepository, KeyBindingCategory category) {
        this.keyBindingRepository = keyBindingRepository;
        this.category = category;
        setDisplayName();
    }

    public ObservableList<KeyBindingViewModel> getChildren() {
        return children;
    }

    public KeyBinding getKeyBinding() {
        return keyBinding;
    }

    public StringProperty shownBindingProperty() {
        return this.shownBinding;
    }

    public String getBinding() {
        return realBinding;
    }

    private void setBinding(String bind) {
        this.realBinding = bind;
        String[] parts = bind.split(" ");
        String displayBind = "";
        for (String part : parts) {
            displayBind += CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, part) + " ";
        }
        this.shownBinding.set(displayBind.trim().replace(" ", " + "));
    }

    private void setDisplayName() {
        this.displayName.set((keyBinding == null) ? this.category.getName() : keyBinding.getLocalization());
    }

    public StringProperty nameProperty() {
        return this.displayName;
    }

    public boolean isCategory() {
        return (keyBinding == null) ? true : false;
    }

    /**
     * Sets a a new key bind to this objects key binding object if
     * the given key event is a valid combination of keys.
     *
     * @param evt as KeyEvent
     * @return true if the KeyEvent is a valid binding, false else
     */
    public boolean setNewBinding(KeyEvent evt) {
        // validate the shortcut is no modifier key

        KeyCode code = evt.getCode();
        if (code.isModifierKey() || (code == KeyCode.BACK_SPACE) || (code == KeyCode.SPACE) || (code == KeyCode.TAB)
                || (code == KeyCode.ENTER) || (code == KeyCode.UNDEFINED)) {
            return false;
        }

        // gather the pressed modifier keys
        String modifiers = "";
        if (evt.isControlDown()) {
            modifiers = "ctrl+";
        }
        if (evt.isShiftDown()) {
            modifiers += "shift+";
        }
        if (evt.isAltDown()) {
            modifiers += "alt+";
        }

        // if no modifier keys are pressed, only special keys can be shortcuts
        if (modifiers.isEmpty()) {
            if (!(code.isFunctionKey() || (code == KeyCode.ESCAPE) || (code == KeyCode.DELETE))) {
                return false;
            }
        }

        String newShortcut = modifiers + code;
        setBinding(newShortcut);

        return true;
    }

    /**
     * This method will reset the key bind of this models KeyBinding object to it's default bind
     */
    public void resetToDefault() {
        if (!isCategory()) {
            String key = getKeyBinding().getConstant();
            keyBindingRepository.resetToDefault(key);
            setBinding(keyBindingRepository.get(key));
        }
    }

    public Optional<IconTheme.JabRefIcon> getIcon() {
        return isCategory() ? Optional.empty() : Optional.of(IconTheme.JabRefIcon.CLEANUP_ENTRIES);
    }
}
