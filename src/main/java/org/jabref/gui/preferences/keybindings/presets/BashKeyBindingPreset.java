package org.jabref.gui.preferences.keybindings.presets;

import java.util.HashMap;
import java.util.Map;

import org.jabref.gui.keyboard.KeyBinding;

public class BashKeyBindingPreset implements KeyBindingPreset {

    private static final Map<KeyBinding, String> KEY_BINDINGS = new HashMap<>();

    static {
        KEY_BINDINGS.put(KeyBinding.EDITOR_DELETE, "ctrl+D");
        // DELETE BACKWARDS = Rubout
        KEY_BINDINGS.put(KeyBinding.EDITOR_BACKWARD, "ctrl+B");
        KEY_BINDINGS.put(KeyBinding.EDITOR_FORWARD, "ctrl+F");
        KEY_BINDINGS.put(KeyBinding.EDITOR_WORD_BACKWARD, "alt+B");
        KEY_BINDINGS.put(KeyBinding.EDITOR_WORD_FORWARD, "alt+F");
        KEY_BINDINGS.put(KeyBinding.EDITOR_BEGINNING, "ctrl+A");
        KEY_BINDINGS.put(KeyBinding.EDITOR_END, "ctrl+E");
        KEY_BINDINGS.put(KeyBinding.EDITOR_BEGINNING_DOC, "alt+LESS");
        KEY_BINDINGS.put(KeyBinding.EDITOR_END_DOC, "alt+shift+LESS");
        KEY_BINDINGS.put(KeyBinding.EDITOR_UP, "ctrl+P");
        KEY_BINDINGS.put(KeyBinding.EDITOR_DOWN, "ctrl+N");
        KEY_BINDINGS.put(KeyBinding.EDITOR_CAPITALIZE, "alt+C");
        KEY_BINDINGS.put(KeyBinding.EDITOR_LOWERCASE, "alt+L");
        KEY_BINDINGS.put(KeyBinding.EDITOR_UPPERCASE, "alt+U");
        KEY_BINDINGS.put(KeyBinding.EDITOR_KILL_LINE, "ctrl+K");
        KEY_BINDINGS.put(KeyBinding.EDITOR_KILL_WORD, "alt+D");
        KEY_BINDINGS.put(KeyBinding.EDITOR_KILL_WORD_BACKWARD, "alt+DELETE");
    }

    @Override
    public String getName() {
        return "Bash";
    }

    @Override
    public Map<KeyBinding, String> getKeyBindings() {
        return KEY_BINDINGS;
    }
}
