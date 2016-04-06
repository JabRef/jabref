package net.sf.jabref.gui.keyboard;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.util.OS;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Objects;
import java.util.SortedMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

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
