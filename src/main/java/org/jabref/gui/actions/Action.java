package org.jabref.gui.actions;

import java.util.Optional;

import org.jabref.gui.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;

public interface Action {
    Optional<JabRefIcon> getIcon();

    Optional<KeyBinding> getKeyBinding();

    String getText();

    String getDescription();
}
