package org.jabref.gui.preferences.keybindings.presets;

import java.util.HashMap;
import java.util.Map;

import org.jabref.gui.keyboard.KeyBinding;

public class BashKeyBindingPreset implements KeyBindingPreset {

    @Override
    public String getName() {
        return "Bash";
    }

    @Override
    public Map<KeyBinding, String> getKeyBindings() {
        final Map<KeyBinding, String> keyBindings = new HashMap<>();

        keyBindings.put(KeyBinding.EDITOR_DELETE, "ctrl+D");
        // DELETE BACKWARDS = Rubout
        keyBindings.put(KeyBinding.EDITOR_BACKWARD, "ctrl+B");
        keyBindings.put(KeyBinding.EDITOR_FORWARD, "ctrl+F");
        keyBindings.put(KeyBinding.EDITOR_WORD_BACKWARD, "alt+B");
        keyBindings.put(KeyBinding.EDITOR_WORD_FORWARD, "alt+F");
        keyBindings.put(KeyBinding.EDITOR_BEGINNING, "ctrl+A");
        keyBindings.put(KeyBinding.EDITOR_END, "ctrl+E");
        keyBindings.put(KeyBinding.EDITOR_BEGINNING_DOC, "alt+LESS");
        keyBindings.put(KeyBinding.EDITOR_END_DOC, "alt+shift+LESS");
        keyBindings.put(KeyBinding.EDITOR_UP, "ctrl+P");
        keyBindings.put(KeyBinding.EDITOR_DOWN, "ctrl+N");
        keyBindings.put(KeyBinding.EDITOR_CAPITALIZE, "alt+C");
        keyBindings.put(KeyBinding.EDITOR_LOWERCASE, "alt+L");
        keyBindings.put(KeyBinding.EDITOR_UPPERCASE, "alt+U");
        keyBindings.put(KeyBinding.EDITOR_KILL_LINE, "ctrl+K");
        keyBindings.put(KeyBinding.EDITOR_KILL_WORD, "alt+D");
        keyBindings.put(KeyBinding.EDITOR_KILL_WORD_BACKWARD, "alt+DELETE");

        return keyBindings;
    }
}
