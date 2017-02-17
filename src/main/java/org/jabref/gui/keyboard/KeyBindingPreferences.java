package org.jabref.gui.keyboard;

import java.awt.AWTError;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.stream.Collectors;

import javax.swing.KeyStroke;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import org.jabref.logic.util.OS;
import org.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class KeyBindingPreferences {

    private static final Log LOGGER = LogFactory.getLog(KeyBindingPreferences.class);

    private int shortcutMask = -1;

    private final JabRefPreferences prefs;

    private KeyBindingRepository keyBindingRepository = new KeyBindingRepository();


    public KeyBindingPreferences(JabRefPreferences prefs) {
        this.prefs = Objects.requireNonNull(prefs);
        restoreKeyBindings();
    }

    /**
     * Returns the KeyStroke for this binding, as defined by the defaults, or in the Preferences.
     */
    public KeyStroke getKey(KeyBinding bindName) {

        String s = keyBindingRepository.get(bindName.getKey());

        if (OS.OS_X) {
            return getKeyForMac(KeyStroke.getKeyStroke(s));
        } else {
            return KeyStroke.getKeyStroke(s);
        }
    }

    public KeyCombination getKeyCombination(KeyBinding bindName) {
        String binding = keyBindingRepository.get(bindName.getKey());
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

    /**
     * Stores new key bindings into Preferences, provided they actually differ from the old ones.
     */
    public void setNewKeyBindings(SortedMap<KeyBinding, String> newBindings) {
        if (!newBindings.equals(keyBindingRepository.getKeyBindings())) {
            // This confirms that the bindings have actually changed.
            List<String> bindNames = newBindings.keySet().stream().map(KeyBinding::getKey).collect(Collectors.toList());
            List<String> bindings = new ArrayList<>(newBindings.values());
            prefs.putStringList(JabRefPreferences.BIND_NAMES, bindNames);
            prefs.putStringList(JabRefPreferences.BINDINGS, bindings);
            keyBindingRepository.overwriteBindings(newBindings);
        }
    }

    private void restoreKeyBindings() {
        // First read the bindings, and their names.
        List<String> bindNames = prefs.getStringList(JabRefPreferences.BIND_NAMES);
        List<String> bindings = prefs.getStringList(JabRefPreferences.BINDINGS);

        // Then set up the key bindings HashMap.
        if ((bindNames.isEmpty()) || (bindings.isEmpty()) || (bindNames.size() != bindings.size())) {
            // Nothing defined in Preferences, or something is wrong.
            keyBindingRepository = new KeyBindingRepository();
            return;
        }

        for (int i = 0; i < bindNames.size(); i++) {
            keyBindingRepository.put(bindNames.get(i), bindings.get(i));
        }
    }


    /**
     * Returns the HashMap containing all key bindings.
     */
    public SortedMap<KeyBinding, String> getKeyBindings() {
        return keyBindingRepository.getKeyBindings();
    }
}
