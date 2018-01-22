package org.jabref.gui.actions;

import java.util.Objects;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jabref.gui.keyboard.KeyBindingRepository;

import org.controlsfx.control.action.ActionUtils;

/**
 * Helper class to create and style controls according to an {@link ActionsFX}.
 */
public class ActionFactory {

    private KeyBindingRepository keyBindingRepository;

    public ActionFactory(KeyBindingRepository keyBindingRepository) {
        this.keyBindingRepository = Objects.requireNonNull(keyBindingRepository);
    }

    public MenuItem configureMenuItem(ActionsFX action, MenuItem menuItem) {
        return ActionUtils.configureMenuItem(new JabRefAction(action, keyBindingRepository), menuItem);
    }

    public MenuItem createMenuItem(ActionsFX action) {
        return ActionUtils.createMenuItem(new JabRefAction(action, keyBindingRepository));
    }

    public Menu createMenu(ActionsFX action) {
        return ActionUtils.createMenu(new JabRefAction(action, keyBindingRepository));
    }
}
