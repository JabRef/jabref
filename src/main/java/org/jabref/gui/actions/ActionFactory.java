package org.jabref.gui.actions;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import javafx.beans.binding.BooleanExpression;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;

import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.model.strings.StringUtil;

import com.sun.javafx.scene.control.ContextMenuContent;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.commands.Command;
import org.controlsfx.control.action.ActionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to create and style controls according to an {@link Action}.
 */
public class ActionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionFactory.class);

    private final KeyBindingRepository keyBindingRepository;

    public ActionFactory(KeyBindingRepository keyBindingRepository) {
        this.keyBindingRepository = Objects.requireNonNull(keyBindingRepository);
    }

    /**
     * For some reason the graphic is not set correctly by the {@link ActionUtils} class, so we have to fix this by hand
     */
    private static void setGraphic(MenuItem node, Action action) {
        node.graphicProperty().unbind();
        action.getIcon().ifPresent(icon -> node.setGraphic(icon.getGraphicNode()));
    }

    /*
     * Returns MenuItemContainer node associated with this menu item
     * which can contain:
     *   1. label node of type Label for displaying menu item text,
     *   2. right node of type Label for displaying accelerator text,
     *      or an arrow if it's a Menu,
     *   3. graphic node for displaying menu item icon, and
     *   4. left node for displaying either radio button or check box.
     *
     * This is basically rewritten impl_styleableGetNode() which
     * should not be used since it's marked as deprecated.
     */
    private static Label getAssociatedNode(MenuItem menuItem) {
        ContextMenuContent.MenuItemContainer container = (ContextMenuContent.MenuItemContainer) menuItem.getStyleableNode();

        if (container == null) {
            return null;
        } else {
            // We have to use reflection to get the associated label
            try {
                Method getLabel = ContextMenuContent.MenuItemContainer.class.getDeclaredMethod("getLabel");
                getLabel.setAccessible(true);
                return (Label) getLabel.invoke(container);
            } catch (InaccessibleObjectException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                LOGGER.warn("Could not get label of menu item", e);
            }
        }
        return null;
    }

    public MenuItem configureMenuItem(Action action, Command command, MenuItem menuItem) {
        ActionUtils.configureMenuItem(new JabRefAction(action, command, keyBindingRepository, Sources.FromMenu), menuItem);
        setGraphic(menuItem, action);

        // Show tooltips
        if (command instanceof SimpleCommand) {
            EasyBind.subscribe(
                    ((SimpleCommand) command).statusMessageProperty(),
                    message -> {
                        Label label = getAssociatedNode(menuItem);
                        if (label != null) {
                            label.setMouseTransparent(false);
                            if (StringUtil.isBlank(message)) {
                                label.setTooltip(null);
                            } else {
                                label.setTooltip(new Tooltip(message));
                            }
                        }
                    }
            );
        }

        return menuItem;
    }

    public MenuItem createMenuItem(Action action, Command command) {
        MenuItem menuItem = new MenuItem();
        configureMenuItem(action, command, menuItem);
        return menuItem;
    }

    public CheckMenuItem createCheckMenuItem(Action action, Command command, boolean selected) {
        CheckMenuItem checkMenuItem = ActionUtils.createCheckMenuItem(new JabRefAction(action, command, keyBindingRepository, Sources.FromMenu));
        checkMenuItem.setSelected(selected);
        setGraphic(checkMenuItem, action);

        return checkMenuItem;
    }

    public CheckMenuItem createCheckMenuItem(Action action, Command command, BooleanExpression selectedBinding) {
        CheckMenuItem checkMenuItem = ActionUtils.createCheckMenuItem(new JabRefAction(action, command, keyBindingRepository, Sources.FromMenu));
        EasyBind.subscribe(selectedBinding, checkMenuItem::setSelected);
        setGraphic(checkMenuItem, action);

        return checkMenuItem;
    }

    public Menu createMenu(Action action) {
        Menu menu = ActionUtils.createMenu(new JabRefAction(action, keyBindingRepository));

        // For some reason the graphic is not set correctly, so let's fix this
        setGraphic(menu, action);
        return menu;
    }

    public Menu createSubMenu(Action action, MenuItem... children) {
        Menu menu = createMenu(action);
        menu.getItems().addAll(children);
        return menu;
    }

    public Button createIconButton(Action action, Command command) {
        Button button = ActionUtils.createButton(new JabRefAction(action, command, keyBindingRepository, Sources.FromButton), ActionUtils.ActionTextBehavior.HIDE);

        button.getStyleClass().setAll("icon-button");

        // For some reason the graphic is not set correctly, so let's fix this
        button.graphicProperty().unbind();
        action.getIcon().ifPresent(icon -> button.setGraphic(icon.getGraphicNode()));

        button.setFocusTraversable(false); // Prevent the buttons from stealing the focus
        return button;
    }

    public ButtonBase configureIconButton(Action action, Command command, ButtonBase button) {
        ActionUtils.unconfigureButton(button);
        ActionUtils.configureButton(
                new JabRefAction(action, command, keyBindingRepository, Sources.FromButton),
                button,
                ActionUtils.ActionTextBehavior.HIDE);

        button.getStyleClass().add("icon-button");

        // For some reason the graphic is not set correctly, so let's fix this
        // ToDO: Find a way to reuse JabRefIconView
        button.graphicProperty().unbind();
        action.getIcon().ifPresent(icon -> button.setGraphic(icon.getGraphicNode()));

        return button;
    }
}
