package org.jabref.gui.actions;

import java.util.Optional;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.stage.WindowEvent;

import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.util.strings.StringUtil;

import com.airhacks.afterburner.injection.Injector;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import de.saxsys.mvvmfx.utils.commands.Command;
import org.controlsfx.control.action.ActionUtils;

/// Helper class to create and style controls according to an [Action].
public class ActionFactory {

    private static final String TOOLTIP_DISPOSER_KEY = ActionFactory.class.getName() + ".tooltipDisposer";

    private final KeyBindingRepository keyBindingRepository;

    public ActionFactory() {
        this.keyBindingRepository = Injector.instantiateModelOrService(KeyBindingRepository.class);
    }

    /// For some reason the graphic is not set correctly by the [ActionUtils] class, so we have to fix this by hand
    private static void setGraphic(MenuItem node, Action action) {
        node.graphicProperty().unbind();
        action.getIcon().ifPresent(icon -> node.setGraphic(icon.getGraphicNode()));
    }

    public MenuItem configureMenuItem(Action action, Command command, MenuItem menuItem) {
        JabRefAction jabRefAction = new JabRefAction(action, command, keyBindingRepository);
        ActionUtils.configureMenuItem(jabRefAction, menuItem);
        setGraphic(menuItem, action);
        enableTooltip(jabRefAction, menuItem);
        return menuItem;
    }

    /// Shows the action's long text (its description and, for a [SimpleCommand], its status message) as tooltip on the menu row.
    ///
    /// [MenuItem] has no tooltip property, unlike the buttons [ActionUtils] configures from the same text.
    /// The row node only exists once the menu has been shown and is recreated when the menu's items change,
    /// so the tooltip is (re)installed whenever the popup is shown.
    private static void enableTooltip(JabRefAction jabRefAction, MenuItem menuItem) {
        // An item can be configured again with another action (e.g., when the push-to-application target changes),
        // so the listeners of the previous configuration have to go, or its outdated tooltip would still be installed
        if (menuItem.getProperties().remove(TOOLTIP_DISPOSER_KEY) instanceof Runnable disposePreviousTooltip) {
            disposePreviousTooltip.run();
        }

        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(jabRefAction.longTextProperty());

        Runnable updateTooltip = () -> {
            // The row is not disabled itself (only styled as such), so it still receives the mouse events a tooltip needs
            Optional.ofNullable(menuItem.getStyleableNode()).ifPresent(row -> {
                // Installing adds the mouse handlers again, so remove any earlier installation first
                Tooltip.uninstall(row, tooltip);
                // Several action descriptions merely repeat the label, which is useless as tooltip
                if (!StringUtil.isBlank(tooltip.getText()) && !tooltip.getText().equals(menuItem.getText())) {
                    Tooltip.install(row, tooltip);
                }
            });
        };
        EventHandler<WindowEvent> onPopupShown = _ -> updateTooltip.run();

        Optional.ofNullable(menuItem.getParentPopup())
                .ifPresent(popup -> popup.addEventHandler(WindowEvent.WINDOW_SHOWN, onPopupShown));
        ChangeListener<ContextMenu> parentPopupListener = (_, oldPopup, newPopup) -> {
            Optional.ofNullable(oldPopup).ifPresent(popup -> popup.removeEventHandler(WindowEvent.WINDOW_SHOWN, onPopupShown));
            Optional.ofNullable(newPopup).ifPresent(popup -> popup.addEventHandler(WindowEvent.WINDOW_SHOWN, onPopupShown));
        };
        menuItem.parentPopupProperty().addListener(parentPopupListener);
        Subscription textSubscription = EasyBind.subscribe(tooltip.textProperty(), _ -> updateTooltip.run());

        Runnable disposeTooltip = () -> {
            textSubscription.unsubscribe();
            menuItem.parentPopupProperty().removeListener(parentPopupListener);
            Optional.ofNullable(menuItem.getParentPopup())
                    .ifPresent(popup -> popup.removeEventHandler(WindowEvent.WINDOW_SHOWN, onPopupShown));
            Optional.ofNullable(menuItem.getStyleableNode()).ifPresent(row -> Tooltip.uninstall(row, tooltip));
            tooltip.textProperty().unbind();
        };
        menuItem.getProperties().put(TOOLTIP_DISPOSER_KEY, disposeTooltip);
    }

    public MenuItem createMenuItem(Action action, Command command) {
        MenuItem menuItem = new MenuItem();
        configureMenuItem(action, command, menuItem);
        return menuItem;
    }

    /// An item for a context menu, carrying no accelerator.
    ///
    /// A [MenuItem]'s accelerator is global: JavaFX keeps one entry per key combination for the
    /// whole scene, so an item in a context menu takes the keystroke away from the main menu's item
    /// for the same action — and then does nothing at all whenever its own command is disabled,
    /// because JavaFX skips a disabled item's accelerator rather than passing the event on.
    ///
    /// That is how Ctrl+Z stopped working outside the entry editor: the search field's context menu
    /// claimed it for `EditorContextAction`, which is enabled only while that field has something to
    /// undo. A context menu is reached with the mouse, so it loses nothing by leaving the key
    /// combination to whoever owns it globally.
    public MenuItem createContextMenuItem(Action action, Command command) {
        MenuItem menuItem = createMenuItem(action, command);
        // configureMenuItem binds the property, so it has to be released before it can be cleared.
        menuItem.acceleratorProperty().unbind();
        menuItem.setAccelerator(null);
        return menuItem;
    }

    public MenuItem createCustomMenuItem(Action action, Command command, String text) {
        MenuItem menuItem = new MenuItem();
        configureMenuItem(action, command, menuItem);
        menuItem.textProperty().unbind();
        menuItem.setText(text);

        return menuItem;
    }

    public CheckMenuItem createCheckMenuItem(Action action, Command command, boolean selected) {
        JabRefAction jabRefAction = new JabRefAction(action, command, keyBindingRepository);
        CheckMenuItem checkMenuItem = ActionUtils.createCheckMenuItem(jabRefAction);
        checkMenuItem.setSelected(selected);
        setGraphic(checkMenuItem, action);
        enableTooltip(jabRefAction, checkMenuItem);

        return checkMenuItem;
    }

    public CheckMenuItem createCheckMenuItem(Action action, Command command, BooleanExpression selectedBinding) {
        JabRefAction jabRefAction = new JabRefAction(action, command, keyBindingRepository);
        CheckMenuItem checkMenuItem = ActionUtils.createCheckMenuItem(jabRefAction);
        EasyBind.subscribe(selectedBinding, checkMenuItem::setSelected);
        setGraphic(checkMenuItem, action);
        enableTooltip(jabRefAction, checkMenuItem);

        return checkMenuItem;
    }

    public Menu createMenu(Action action) {
        JabRefAction jabRefAction = new JabRefAction(action, keyBindingRepository);
        Menu menu = ActionUtils.createMenu(jabRefAction);

        // For some reason the graphic is not set correctly, so let's fix this
        setGraphic(menu, action);
        enableTooltip(jabRefAction, menu);
        return menu;
    }

    public Menu createSubMenu(Action action, MenuItem... children) {
        Menu menu = createMenu(action);
        menu.getItems().addAll(children);
        return menu;
    }

    public Button createIconButton(Action action, Command command) {
        Button button = ActionUtils.createButton(new JabRefAction(action, command, keyBindingRepository), ActionUtils.ActionTextBehavior.HIDE);

        initButton(action, button);

        return button;
    }

    public ButtonBase configureIconButton(Action action, Command command, ButtonBase button) {
        ActionUtils.unconfigureButton(button);
        ActionUtils.configureButton(
                new JabRefAction(action, command, keyBindingRepository),
                button,
                ActionUtils.ActionTextBehavior.HIDE);

        initButton(action, button);

        return button;
    }

    private static void initButton(Action action, ButtonBase button) {
        button.setFocusTraversable(true);
        button.getStyleClass().add("icon-button");
        button.graphicProperty().unbind();
        action.getIcon().ifPresent(icon -> button.setGraphic(icon.getGraphicNode()));
    }
}
