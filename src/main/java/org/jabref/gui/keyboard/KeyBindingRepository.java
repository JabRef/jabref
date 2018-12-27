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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyBindingRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyBindingRepository.class);
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
                put(keyBinding, keyBinding.getDefaultKeyBinding());
            }
        } else {
            for (int i = 0; i < bindNames.size(); i++) {
                put(bindNames.get(i), bindings.get(i));
            }
        }
    }

    /**
     * Check if the given keyCombination equals the given keyEvent
     *
     * @param combination as KeyCombination
     * @param keyEvent    as KeEvent
     * @return true if matching, else false
     */
    public static boolean checkKeyCombinationEquality(KeyCombination combination, KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();
        if (code == KeyCode.UNDEFINED) {
            return false;
        }

        return combination.match(keyEvent);
    }

    public Optional<String> get(KeyBinding key) {
        return Optional.ofNullable(bindings.get(key));
    }

    public String get(String key) {
        Optional<KeyBinding> keyBinding = getKeyBinding(key);
        Optional<String> result = keyBinding.flatMap(k -> Optional.ofNullable(bindings.get(k)));

        if (result.isPresent()) {
            return result.get();
        } else if (keyBinding.isPresent()) {
            return keyBinding.get().getDefaultKeyBinding();
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
        bindings.put(key, value);
    }

    public void put(String key, String value) {
        getKeyBinding(key).ifPresent(binding -> put(binding, value));
    }

    private Optional<KeyBinding> getKeyBinding(String key) {
        return Arrays.stream(KeyBinding.values()).filter(b -> b.getConstant().equals(key)).findFirst();
    }

    public void resetToDefault(String key) {
        getKeyBinding(key).ifPresent(b -> bindings.put(b, b.getDefaultKeyBinding()));
    }

    public void resetToDefault() {
        bindings.forEach((b, s) -> bindings.put(b, b.getDefaultKeyBinding()));
    }

    public int size() {
        return this.bindings.size();
    }

    public Optional<KeyBinding> mapToKeyBinding(KeyEvent keyEvent) {
        for (KeyBinding binding : KeyBinding.values()) {
            if (checkKeyCombinationEquality(binding, keyEvent)) {
                return Optional.of(binding);
            }
        }
        return Optional.empty();
    }

    public Optional<KeyBinding> mapToKeyBinding(java.awt.event.KeyEvent keyEvent) {
        Optional<KeyCode> keyCode = Arrays.stream(KeyCode.values()).filter(k -> k.impl_getCode() == keyEvent.getKeyCode()).findFirst();
        if (keyCode.isPresent()) {
            KeyEvent event = new KeyEvent(keyEvent.getSource(), null, KeyEvent.KEY_PRESSED, "", "", keyCode.get(), keyEvent.isShiftDown(), keyEvent.isControlDown(), keyEvent.isAltDown(), keyEvent.isMetaDown());
            return mapToKeyBinding(event);

        }

        return Optional.empty();

    }

    /**
     * Returns the KeyStroke for this binding, as defined by the defaults, or in the Preferences.
     */
    public KeyStroke getKey(KeyBinding bindName) {
        String s = get(bindName.getConstant());
        s = s.replace("+", " "); //swing needs the keys without pluses but whitespace between the modifiers

        if (OS.OS_X) {
            return getKeyForMac(KeyStroke.getKeyStroke(s));
        } else {
            return KeyStroke.getKeyStroke(s);
        }
    }

    public KeyCombination getKeyCombination(KeyBinding bindName) {
        String binding = get(bindName.getConstant());
        if (OS.OS_X) {
            binding = binding.replace("ctrl", "meta");
        }

        return KeyCombination.valueOf(binding);
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
        return bindings.keySet().stream().map(KeyBinding::getConstant).collect(Collectors.toList());
    }

    public List<String> getBindings() {
        return new ArrayList<>(bindings.values());
    }
}
