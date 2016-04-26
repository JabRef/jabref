/*  Copyright (C) 2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import net.sf.jabref.JabRefGUI;

/**
 * This class shall provide a super class for future dialogs implemented in java fx.
 * It mimics the behavior of a swing JDialog which means once a object of this class
 * is shown all swing windows will be blocked and stay in the background. Since this
 * class extends from a java fx Alert it behaves as a normal dialog towards all
 * windows in the java fx thread.
 * <p>To create a custom java fx dialog one should extend this class and set a dialog
 * pane through the inherited {@link setDialogPane(DialogPane)} method in the constructor.
 * The layout of the pane should be define in an external fxml file and loaded it via the
 * {@link FXMLLoader}.
 *
 */
public class FXAlert extends Alert {

    /**
     * The WindowAdapter will be added to all swing windows once an instance
     * of this class is shown and redirects the focus towards this instance.
     * It will be removed once the instance of this class gets hidden.
     *
     */
    private final WindowAdapter fxOverSwingHelper = new WindowAdapter() {

        @Override
        public void windowActivated(WindowEvent e) {
            Platform.runLater(() -> {
                Stage fxDialogWindow = getDialogWindow();
                fxDialogWindow.toFront();
                fxDialogWindow.requestFocus();
            });
        }

        @Override
        public void windowGainedFocus(WindowEvent e) {
            Platform.runLater(() -> {
                Stage fxDialogWindow = getDialogWindow();
                fxDialogWindow.toFront();
                fxDialogWindow.requestFocus();
            });
        }
    };

    public FXAlert(AlertType type, String title, Image image) {
        this(type, title);
        setDialogIcon(image);
    }

    public FXAlert(AlertType type, String title) {
        this(type);
        setTitle(title);
    }

    public FXAlert(AlertType type) {
        super(type);
        Stage fxDialogWindow = getDialogWindow();

        fxDialogWindow.setOnShown(evt -> {
            setSwingWindowsEnabledAndFocusable(false);
            setLocationRelativeToMainWindow();
        });

        fxDialogWindow.setOnHiding(evt -> setSwingWindowsEnabledAndFocusable(true));

        fxDialogWindow.setOnCloseRequest(evt -> this.close());
    }

    public void setDialogStyle(String pathToStyleSheet) {
        getDialogPane().getScene().getStylesheets().add(pathToStyleSheet);
    }

    public void setDialogIcon(Image image) {
        Stage fxDialogWindow = getDialogWindow();
        fxDialogWindow.getIcons().add(image);
    }

    private Stage getDialogWindow() {
        return (Stage) getDialogPane().getScene().getWindow();
    }

    private void setSwingWindowsEnabledAndFocusable(boolean enabled) {
        for (Window swingWindow : Window.getWindows()) {
            swingWindow.setEnabled(enabled);
            if (!enabled) {
                swingWindow.addWindowListener(fxOverSwingHelper);
            } else {
                swingWindow.removeWindowListener(fxOverSwingHelper);
            }
        }
    }

    private void setLocationRelativeToMainWindow() {
        double mainWindowX = JabRefGUI.getMainFrame().getLocationOnScreen().getX();
        double mainWindowY = JabRefGUI.getMainFrame().getLocationOnScreen().getY();
        double mainWindowWidth = JabRefGUI.getMainFrame().getSize().getWidth();
        double mainWindowHeight = JabRefGUI.getMainFrame().getSize().getHeight();

        setX((mainWindowX + (mainWindowWidth / 2)) - (getWidth() / 2));
        setY((mainWindowY + (mainWindowHeight / 2)) - (getHeight() / 2));
    }

}
