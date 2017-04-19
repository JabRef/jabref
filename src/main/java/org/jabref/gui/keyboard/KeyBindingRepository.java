package org.jabref.gui.keyboard;

import java.awt.AWTError;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.swing.KeyStroke;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import org.jabref.logic.util.OS;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class KeyBindingRepository {

    private static final Log LOGGER = LogFactory.getLog(KeyBindingRepository.class);
    /**
     * sorted by localization
     */
    private final SortedMap<KeyBinding, String> bindings;
    private int shortcutMask = -1;

    public KeyBindingRepository() {
        this(Collections.emptyList(), Collections.emptyList());
    }

    public KeyBindingRepository(List<String> bindNames, List<String> bindings) {
        this.bindings = new TreeMap<>(Comparator.comparing(KeyBinding::getLocalization));

        if ((bindNames.isEmpty()) || (bindings.isEmpty()) || (bindNames.size() != bindings.size())) {
            // Use default key bindings
            for (KeyBinding keyBinding : KeyBinding.values()) {
                put(keyBinding, keyBinding.getDefaultBinding());
            }
        } else {
            for (int i = 0; i < bindNames.size(); i++) {
                put(bindNames.get(i), bindings.get(i));
            }
        }
    }

    public Optional<String> get(KeyBinding key) {
        return getKeyBinding(key).flatMap(k -> Optional.ofNullable(bindings.get(k)));
    }

    public String get(String key) {
        Optional<KeyBinding> keyBinding = getKeyBinding(key);
        Optional<String> result = keyBinding.flatMap(k -> Optional.ofNullable(bindings.get(k)));

        if (result.isPresent()) {
            return result.get();
        } else if (keyBinding.isPresent()) {
            return keyBinding.get().getDefaultBinding();
        } else {
            return "Not associated";
        }
    }

    /**
     * Returns the HashMap containing all key bindings.
     */
    public SortedMap<KeyBinding, String> getKeyBindings() {
        return new TreeMap<>(bindings);
    }

    public void put(KeyBinding key, String value) {
        getKeyBinding(key).ifPresent(binding -> bindings.put(binding, value));
    }

    public void put(String key, String value) {
        getKeyBinding(key).ifPresent(binding -> bindings.put(binding, value));
    }

    private Optional<KeyBinding> getKeyBinding(String key) {
        return Arrays.stream(KeyBinding.values()).filter(b -> b.getKey().equals(key)).findFirst();
    }

    private Optional<KeyBinding> getKeyBinding(KeyBinding key) {
        return Arrays.stream(KeyBinding.values()).filter(b -> b.equals(key)).findFirst();
    }

    public void resetToDefault(String key) {
        getKeyBinding(key).ifPresent(b -> bindings.put(b, b.getDefaultBinding()));
    }

    public void resetToDefault() {
        bindings.forEach((b, s) -> bindings.put(b, b.getDefaultBinding()));
    }

    public int size() {
        return this.bindings.size();
    }

    /**
     * Returns the KeyStroke for this binding, as defined by the defaults, or in the Preferences.
     */
    public KeyStroke getKey(KeyBinding bindName) {

        String s = get(bindName.getKey());

        if (OS.OS_X) {
            return getKeyForMac(KeyStroke.getKeyStroke(s));
        } else {
            return KeyStroke.getKeyStroke(s);
        }
    }

    private KeyCombination getKeyCombination(KeyBinding bindName) {
        String binding = get(bindName.getKey());
        return KeyCombination.valueOf(binding);
    }

    /**
     * Check if the given keyCombination equals the given keyEvent
     *
     * @param combination as KeyCombination
     * @param keyEvent as KeEvent
     * @return true if matching, else false
     */
    public boolean checkKeyCombinationEquality(KeyCombination combination, KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();
        if (code == KeyCode.UNDEFINED) {
            return false;
        }
        // gather the pressed modifier keys
        String modifiers = "";
        if (keyEvent.isControlDown()) {
            modifiers = "ctrl";
        }
        if (keyEvent.isShiftDown()) {
            modifiers += " shift";
        }
        if (keyEvent.isAltDown()) {
            modifiers += " alt";
        }
        modifiers = modifiers.trim();
        String newShortcut = (modifiers.isEmpty()) ? code.toString() : modifiers + " " + code;
        KeyCombination pressedCombination = KeyCombination.valueOf(newShortcut);
        return combination.equals(pressedCombination);
    }

    /**
     * Check if the given KeyBinding equals the given keyEvent
     *
     * @param binding as KeyBinding
     * @param keyEvent as KeEvent
     * @return true if matching, else false
     */
    public boolean checkKeyCombinationEquality(KeyBinding binding, KeyEvent keyEvent) {
        KeyCombination keyCombination = getKeyCombination(binding);
        return checkKeyCombinationEquality(keyCombination, keyEvent);
    }

    /**
     * Returns the KeyStroke for this binding, as defined by the defaults, or in the Preferences, but adapted for Mac
     * users, with the Command key preferred instead of Control.
     * TODO: Move to OS.java? Or replace with portable Java key codes, i.e. KeyEvent
     */
    private KeyStroke getKeyForMac(KeyStroke ks) {
        if (ks == null) {
            return null;
        }
        int keyCode = ks.getKeyCode();
        if ((ks.getModifiers() & InputEvent.CTRL_MASK) == 0) {
            return ks;
        } else {
            int modifiers = 0;
            if ((ks.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                modifiers = modifiers | InputEvent.SHIFT_MASK;
            }
            if ((ks.getModifiers() & InputEvent.ALT_MASK) != 0) {
                modifiers = modifiers | InputEvent.ALT_MASK;
            }

            if (shortcutMask == -1) {
                try {
                    shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                } catch (AWTError | HeadlessException e) {
                    LOGGER.warn("Problem geting shortcut mask", e);
                }
            }

            return KeyStroke.getKeyStroke(keyCode, shortcutMask + modifiers);
        }
    }

    public List<String> getBindNames() {
        return bindings.keySet().stream().map(KeyBinding::getKey).collect(Collectors.toList());
    }

    public List<String> getBindings() {
        return new ArrayList<>(bindings.values());
    }
}
