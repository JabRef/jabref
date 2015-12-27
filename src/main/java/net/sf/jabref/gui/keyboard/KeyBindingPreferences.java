package net.sf.jabref.gui.keyboard;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.util.OS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

public class KeyBindingPreferences {

    private int SHORTCUT_MASK = -1;

    private final JabRefPreferences prefs;

    public KeyBindingPreferences(JabRefPreferences prefs) {
        this.prefs = Objects.requireNonNull(prefs);
        restoreKeyBindings();
    }

    private KeyBindingRepository keyBindingRepository = new KeyBindingRepository();

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

            if (SHORTCUT_MASK == -1) {
                try {
                    SHORTCUT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                } catch (Throwable ignored) {
                    // Ignored
                }
            }

            return KeyStroke.getKeyStroke(keyCode, SHORTCUT_MASK + modifiers);
        }
    }


    /**
     * Stores new key bindings into Preferences, provided they actually differ from the old ones.
     */
    public void setNewKeyBindings(SortedMap<KeyBinding, String> newBindings) {
        if (!newBindings.equals(keyBindingRepository.getKeyBindings())) {
            // This confirms that the bindings have actually changed.
            String[] bindNames = new String[newBindings.size()];
            String[] bindings = new String[newBindings.size()];
            int index = 0;
            for (Map.Entry<KeyBinding, String> keyBinding : newBindings.entrySet()) {
                bindNames[index] = keyBinding.getKey().getKey();
                bindings[index] = keyBinding.getValue();
                index++;
            }
            prefs.putStringArray(JabRefPreferences.BIND_NAMES, bindNames);
            prefs.putStringArray(JabRefPreferences.BINDINGS, bindings);
            keyBindingRepository.overwriteBindings(newBindings);
        }
    }

    private void restoreKeyBindings() {
        // First read the bindings, and their names.
        String[] bindNames = prefs.getStringArray(JabRefPreferences.BIND_NAMES);
        String[] bindings = prefs.getStringArray(JabRefPreferences.BINDINGS);

        // Then set up the key bindings HashMap.
        if ((bindNames == null) || (bindings == null)
                || (bindNames.length != bindings.length)) {
            // Nothing defined in Preferences, or something is wrong.
            keyBindingRepository = new KeyBindingRepository();
            return;
        }

        for (int i = 0; i < bindNames.length; i++) {
            keyBindingRepository.put(bindNames[i], bindings[i]);
        }
    }


    /**
     * Returns the HashMap containing all key bindings.
     */
    public SortedMap<KeyBinding, String> getKeyBindings() {
        return keyBindingRepository.getKeyBindings();
    }
}
