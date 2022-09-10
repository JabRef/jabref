package org.jabref.gui.actions;

import java.util.Optional;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;

public interface Action {
    default Optional<JabRefIcon> getIcon() {
        return Optional.empty();
    }

    default Optional<KeyBinding> getKeyBinding() {
        return Optional.empty();
    }

    String getText();

    default String getDescription() {
        return "";
    }
}
