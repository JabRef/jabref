package org.jabref.gui.preferences.keybindings.presets;

import java.util.HashMap;
import java.util.Map;

import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.os.OS;

public class BashKeyBindingPreset implements KeyBindingPreset {

    @Override
    public String getName() {
        return "Bash";
    }

    @Override
    // [impl->req~macos.text-editing-shortcuts~1]
    public Map<KeyBinding, String> getKeyBindings() {
        final Map<KeyBinding, String> keyBindings = new HashMap<>();

        if (OS.OS_X) {
            keyBindings.put(KeyBinding.EDITOR_DELETE, "ctrl+D");
            keyBindings.put(KeyBinding.EDITOR_BACKWARD, "ctrl+B");
            keyBindings.put(KeyBinding.EDITOR_FORWARD, "ctrl+F");
            keyBindings.put(KeyBinding.EDITOR_WORD_BACKWARD, "alt+LEFT");
            keyBindings.put(KeyBinding.EDITOR_WORD_FORWARD, "alt+RIGHT");
            keyBindings.put(KeyBinding.EDITOR_BEGINNING, "ctrl+A");
            keyBindings.put(KeyBinding.EDITOR_END, "ctrl+E");
            keyBindings.put(KeyBinding.EDITOR_BEGINNING_DOC, "shortcut+UP");
            keyBindings.put(KeyBinding.EDITOR_END_DOC, "shortcut+DOWN");
            keyBindings.put(KeyBinding.EDITOR_UP, "ctrl+P");
            keyBindings.put(KeyBinding.EDITOR_DOWN, "ctrl+N");
            keyBindings.put(KeyBinding.EDITOR_KILL_LINE, "ctrl+K");
            keyBindings.put(KeyBinding.EDITOR_KILL_WORD_BACKWARD, "alt+BACK_SPACE");
            keyBindings.put(KeyBinding.EDITOR_CENTER_CARET, "ctrl+L");
            keyBindings.put(KeyBinding.EDITOR_INSERT_LINE_AFTER, "ctrl+O");
            keyBindings.put(KeyBinding.EDITOR_TRANSPOSE_CHARACTERS, "ctrl+T");
        } else {
            keyBindings.put(KeyBinding.EDITOR_DELETE, "shortcut+D");
            // DELETE BACKWARDS = Rubout
            keyBindings.put(KeyBinding.EDITOR_BACKWARD, "shortcut+B");
            keyBindings.put(KeyBinding.EDITOR_FORWARD, "shortcut+F");
            keyBindings.put(KeyBinding.EDITOR_WORD_BACKWARD, "alt+B");
            keyBindings.put(KeyBinding.EDITOR_WORD_FORWARD, "alt+F");
            keyBindings.put(KeyBinding.EDITOR_BEGINNING, "shortcut+A");
            keyBindings.put(KeyBinding.EDITOR_END, "shortcut+E");
            keyBindings.put(KeyBinding.EDITOR_BEGINNING_DOC, "alt+LESS");
            keyBindings.put(KeyBinding.EDITOR_END_DOC, "alt+shift+LESS");
            keyBindings.put(KeyBinding.EDITOR_UP, "shortcut+P");
            keyBindings.put(KeyBinding.EDITOR_DOWN, "shortcut+N");
            keyBindings.put(KeyBinding.EDITOR_CAPITALIZE, "alt+C");
            keyBindings.put(KeyBinding.EDITOR_LOWERCASE, "alt+L");
            keyBindings.put(KeyBinding.EDITOR_UPPERCASE, "alt+U");
            keyBindings.put(KeyBinding.EDITOR_KILL_LINE, "shortcut+K");
            keyBindings.put(KeyBinding.EDITOR_KILL_WORD, "alt+D");
            keyBindings.put(KeyBinding.EDITOR_KILL_WORD_BACKWARD, "alt+DELETE");
        }

        return keyBindings;
    }
}
