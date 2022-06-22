package org.jabref.gui.fieldeditors;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.Screen;
import javafx.stage.Window;

import com.sun.javafx.scene.control.Properties;

/**
 * This class contains some code taken from {@link com.sun.javafx.scene.control.behavior.TextInputControlBehavior},
 * witch is not accessible and thus we have no other choice.
 * TODO: remove this ugly workaround as soon as control behavior is made public
 * reported at https://github.com/javafxports/openjdk-jfx/issues/583
 */
public class TextInputControlBehavior {

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
