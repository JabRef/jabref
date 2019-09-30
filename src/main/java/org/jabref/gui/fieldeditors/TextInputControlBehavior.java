package org.jabref.gui.fieldeditors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import org.jabref.logic.l10n.Localization;
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

    /**
     * Returns the default context menu items (except undo/redo)
     */
    public static List<MenuItem> getDefaultContextMenuItems(TextInputControl textInputControl) {
        boolean editable = textInputControl.isEditable();
        boolean hasText = (textInputControl.getLength() > 0);
        boolean hasSelection = (textInputControl.getSelection().getLength() > 0);
        boolean allSelected = (textInputControl.getSelection().getLength() == textInputControl.getLength());
        boolean maskText = (textInputControl instanceof PasswordField); // (maskText("A") != "A");
        ArrayList<MenuItem> items = new ArrayList<>();

        MenuItem cutMI = new MenuItem(Localization.lang("Cut"));
        cutMI.setOnAction(e -> textInputControl.cut());
        MenuItem copyMI = new MenuItem(Localization.lang("Copy"));
        copyMI.setOnAction(e -> textInputControl.copy());
        MenuItem pasteMI = new MenuItem(Localization.lang("Paste"));
        pasteMI.setOnAction(e -> textInputControl.paste());
        MenuItem deleteMI = new MenuItem(Localization.lang("Delete"));
        deleteMI.setOnAction(e -> {
            IndexRange selection = textInputControl.getSelection();
            textInputControl.deleteText(selection);
        });
        MenuItem selectAllMI = new MenuItem(Localization.lang("Select all"));
        selectAllMI.setOnAction(e -> textInputControl.selectAll());
        MenuItem separatorMI = new SeparatorMenuItem();

        if (SHOW_HANDLES) {
            if (!maskText && hasSelection) {
                if (editable) {
                    items.add(cutMI);
                }
                items.add(copyMI);
            }
            if (editable && Clipboard.getSystemClipboard().hasString()) {
                items.add(pasteMI);
            }
            if (hasText && !allSelected) {
                items.add(selectAllMI);
            }
            selectAllMI.getProperties().put("refreshMenu", Boolean.TRUE);
        } else {
            if (editable) {
                items.addAll(Arrays.asList(cutMI, copyMI, pasteMI, deleteMI, separatorMI, selectAllMI));
            } else {
                items.addAll(Arrays.asList(copyMI, separatorMI, selectAllMI));
            }
            cutMI.setDisable(maskText || !hasSelection);
            copyMI.setDisable(maskText || !hasSelection);
            pasteMI.setDisable(!Clipboard.getSystemClipboard().hasString());
            deleteMI.setDisable(!hasSelection);
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
    }
}
