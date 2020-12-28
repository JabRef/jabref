package org.jabref.gui.fieldeditors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.Screen;
import javafx.stage.Window;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.util.OS;

import com.sun.javafx.scene.control.Properties;

/**
 * This class contains some code taken from {@link com.sun.javafx.scene.control.behavior.TextInputControlBehavior},
 * witch is not accessible and thus we have no other choice.
 * TODO: remove this ugly workaround as soon as control behavior is made public
 * reported at https://github.com/javafxports/openjdk-jfx/issues/583
 */
public class TextInputControlBehavior {

    private static final boolean SHOW_HANDLES = Properties.IS_TOUCH_SUPPORTED && !OS.OS_X;

    private static class EditAction extends SimpleCommand {

        private final StandardActions command;
        private final TextInputControl textInputControl;

        public EditAction(StandardActions command, TextInputControl textInputControl) {
            this.command = command;
            this.textInputControl = textInputControl;

            BooleanProperty editableBinding = textInputControl.editableProperty();
            BooleanBinding hasTextBinding = Bindings.createBooleanBinding(() -> textInputControl.getLength() > 0, textInputControl.textProperty());
            BooleanProperty hasStringInClipboardBinding = new SimpleBooleanProperty(Clipboard.getSystemClipboard().hasString());
            BooleanBinding hasSelectionBinding = Bindings.createBooleanBinding(() -> textInputControl.getSelection().getLength() > 0, textInputControl.selectionProperty());
            BooleanBinding allSelectedBinding = Bindings.createBooleanBinding(() -> textInputControl.getSelection().getLength() == textInputControl.getLength());
            BooleanBinding maskTextBinding = Bindings.createBooleanBinding(() -> textInputControl instanceof PasswordField, BindingsHelper.constantOf(true)); // (maskText("A") != "A");

            if (SHOW_HANDLES) {
                this.executable.bind(
                        switch (command) {
                            case COPY -> editableBinding.and(maskTextBinding.not()).and(hasSelectionBinding);
                            case CUT -> maskTextBinding.not().and(hasSelectionBinding);
                            case PASTE -> editableBinding.and(hasStringInClipboardBinding);
                            case DELETE -> editableBinding.and(hasSelectionBinding);
                            case SELECT_ALL -> hasTextBinding.and(allSelectedBinding.not());
                            default -> BindingsHelper.constantOf(true);
                        });
            } else {
                this.executable.bind(
                        switch (command) {
                            case COPY -> editableBinding.and(maskTextBinding.not()).and(hasSelectionBinding);
                            case CUT -> maskTextBinding.not().and(hasSelectionBinding);
                            case PASTE -> editableBinding.and(hasStringInClipboardBinding);
                            case DELETE -> editableBinding.and(hasSelectionBinding);
                            case SELECT_ALL -> hasTextBinding.and(allSelectedBinding.not()); // why was this disabled before?
                            default -> BindingsHelper.constantOf(true);
                        });
            }
        }

        @Override
        public void execute() {
            switch (command) {
                case COPY -> textInputControl.copy();
                case CUT -> textInputControl.cut();
                case PASTE -> textInputControl.paste();
                case DELETE -> {
                    IndexRange selection = textInputControl.getSelection();
                    textInputControl.deleteText(selection);
                }
                case SELECT_ALL -> textInputControl.selectAll();
            }
            textInputControl.requestFocus();
        }
    }

    /**
     * Returns the default context menu items (except undo/redo)
     */
    public static List<MenuItem> getDefaultContextMenuItems(TextInputControl textInputControl,
                                                            KeyBindingRepository keyBindingRepository) {
        ActionFactory factory = new ActionFactory(keyBindingRepository);

        MenuItem cutMenuItem = factory.createMenuItem(
                StandardActions.CUT,
                new EditAction(StandardActions.CUT, textInputControl));
        MenuItem copyMenuItem = factory.createMenuItem(
                StandardActions.COPY,
                new EditAction(StandardActions.COPY, textInputControl));
        MenuItem pasteMenuItem = factory.createMenuItem(
                StandardActions.PASTE,
                new EditAction(StandardActions.PASTE, textInputControl));
        MenuItem deleteMenuItem = factory.createMenuItem(
                StandardActions.DELETE,
                new EditAction(StandardActions.DELETE, textInputControl));
        MenuItem selectAllMenuItem = factory.createMenuItem(
                StandardActions.SELECT_ALL,
                new EditAction(StandardActions.SELECT_ALL, textInputControl));

        ArrayList<MenuItem> items = new ArrayList<>(Arrays.asList(
                cutMenuItem,
                copyMenuItem,
                pasteMenuItem,
                deleteMenuItem, // should be disabled
                new SeparatorMenuItem(),
                selectAllMenuItem
        ));

        if (SHOW_HANDLES) {
            selectAllMenuItem.getProperties().put("refreshMenu", Boolean.TRUE); // what does that mean?
        }

        return items;
    }

    /**
     * @implNote taken from {@link com.sun.javafx.scene.control.behavior.TextFieldBehavior#contextMenuRequested(javafx.scene.input.ContextMenuEvent)}
     */
    public static void showContextMenu(TextField textField, ContextMenu contextMenu, ContextMenuEvent e) {
        double screenX = e.getScreenX();
        double screenY = e.getScreenY();
        double sceneX = e.getSceneX();

        TextFieldSkin skin = (TextFieldSkin) textField.getSkin();

        if (Properties.IS_TOUCH_SUPPORTED) {
            Point2D menuPos;
            if (textField.getSelection().getLength() == 0) {
                skin.positionCaret(skin.getIndex(e.getX(), e.getY()), false);
                menuPos = skin.getMenuPosition();
            } else {
                menuPos = skin.getMenuPosition();
                if (menuPos != null && (menuPos.getX() <= 0 || menuPos.getY() <= 0)) {
                    skin.positionCaret(skin.getIndex(e.getX(), e.getY()), false);
                    menuPos = skin.getMenuPosition();
                }
            }

            if (menuPos != null) {
                Point2D p = textField.localToScene(menuPos);
                Scene scene = textField.getScene();
                Window window = scene.getWindow();
                Point2D location = new Point2D(window.getX() + scene.getX() + p.getX(),
                        window.getY() + scene.getY() + p.getY());
                screenX = location.getX();
                sceneX = p.getX();
                screenY = location.getY();
            }
        }

        double menuWidth = contextMenu.prefWidth(-1);
        double menuX = screenX - (Properties.IS_TOUCH_SUPPORTED ? (menuWidth / 2) : 0);
        Screen currentScreen = Screen.getPrimary();
        Rectangle2D bounds = currentScreen.getBounds();

        if (menuX < bounds.getMinX()) {
            textField.getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
            textField.getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
            contextMenu.show(textField, bounds.getMinX(), screenY);
        } else if (screenX + menuWidth > bounds.getMaxX()) {
            double leftOver = menuWidth - (bounds.getMaxX() - screenX);
            textField.getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
            textField.getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
            contextMenu.show(textField, screenX - leftOver, screenY);
        } else {
            textField.getProperties().put("CONTEXT_MENU_SCREEN_X", 0);
            textField.getProperties().put("CONTEXT_MENU_SCENE_X", 0);
            contextMenu.show(textField, menuX, screenY);
        }

        e.consume();
    }

    /**
     * @implNote taken from {@link com.sun.javafx.scene.control.behavior.TextAreaBehavior#contextMenuRequested(javafx.scene.input.ContextMenuEvent)}
     */
    public static void showContextMenu(TextArea textArea, ContextMenu contextMenu, ContextMenuEvent e) {
        double screenX = e.getScreenX();
        double screenY = e.getScreenY();
        double sceneX = e.getSceneX();

        TextAreaSkin skin = (TextAreaSkin) textArea.getSkin();

        if (Properties.IS_TOUCH_SUPPORTED) {
            Point2D menuPos;
            if (textArea.getSelection().getLength() == 0) {
                skin.positionCaret(skin.getIndex(e.getX(), e.getY()), false);
                menuPos = skin.getMenuPosition();
            } else {
                menuPos = skin.getMenuPosition();
                if (menuPos != null && (menuPos.getX() <= 0 || menuPos.getY() <= 0)) {
                    skin.positionCaret(skin.getIndex(e.getX(), e.getY()), false);
                    menuPos = skin.getMenuPosition();
                }
            }

            if (menuPos != null) {
                Point2D p = textArea.localToScene(menuPos);
                Scene scene = textArea.getScene();
                Window window = scene.getWindow();
                Point2D location = new Point2D(window.getX() + scene.getX() + p.getX(),
                        window.getY() + scene.getY() + p.getY());
                screenX = location.getX();
                sceneX = p.getX();
                screenY = location.getY();
            }
        }

        double menuWidth = contextMenu.prefWidth(-1);
        double menuX = screenX - (Properties.IS_TOUCH_SUPPORTED ? (menuWidth / 2) : 0);
        Screen currentScreen = Screen.getPrimary();
        Rectangle2D bounds = currentScreen.getBounds();

        if (menuX < bounds.getMinX()) {
            textArea.getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
            textArea.getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
            contextMenu.show(textArea, bounds.getMinX(), screenY);
        } else if (screenX + menuWidth > bounds.getMaxX()) {
            double leftOver = menuWidth - (bounds.getMaxX() - screenX);
            textArea.getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
            textArea.getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
            contextMenu.show(textArea, screenX - leftOver, screenY);
        } else {
            textArea.getProperties().put("CONTEXT_MENU_SCREEN_X", 0);
            textArea.getProperties().put("CONTEXT_MENU_SCENE_X", 0);
            contextMenu.show(textArea, menuX, screenY);
        }

        e.consume();
    }
}
