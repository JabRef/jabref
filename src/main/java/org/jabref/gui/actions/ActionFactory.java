package org.jabref.gui.actions;

import java.util.Objects;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jabref.gui.keyboard.KeyBindingRepository;

import de.saxsys.mvvmfx.utils.commands.Command;
import org.controlsfx.control.action.ActionUtils;

/**
 * Helper class to create and style controls according to an {@link ActionsFX}.
 */
public class ActionFactory {

    private KeyBindingRepository keyBindingRepository;

    public ActionFactory(KeyBindingRepository keyBindingRepository) {
        this.keyBindingRepository = Objects.requireNonNull(keyBindingRepository);
    }

    public MenuItem configureMenuItem(ActionsFX action, Command command, MenuItem menuItem) {
        return ActionUtils.configureMenuItem(new JabRefAction(action, command, keyBindingRepository), menuItem);
    }

    public MenuItem createMenuItem(ActionsFX action, Command command) {
        MenuItem menuItem = ActionUtils.createMenuItem(new JabRefAction(action, command, keyBindingRepository));

        // For some reason the graphic is not set correctly, so let's fix this
        menuItem.graphicProperty().unbind();
        action.getIcon().ifPresent(icon -> menuItem.setGraphic(icon.getGraphicNode()));
        return menuItem;
    }

    public Menu createMenu(ActionsFX action) {
        Menu menu = ActionUtils.createMenu(new JabRefAction(action, keyBindingRepository));

        // For some reason the graphic is not set correctly, so let's fix this
        menu.graphicProperty().unbind();
        action.getIcon().ifPresent(icon -> menu.setGraphic(icon.getGraphicNode()));
        return menu;
    }

    public Menu createSubMenu(ActionsFX action, MenuItem... children) {
        Menu menu = createMenu(action);
        menu.getItems().addAll(children);
        return menu;
    }
}
