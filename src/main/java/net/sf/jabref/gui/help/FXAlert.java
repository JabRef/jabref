/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui.help;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.sf.jabref.JabRef;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * This class shall provide a wrapper class for future dialogs implemented in java fx.
 * It mimics the behaviour of a swing dialog which means once a object of this class
 * is shown all swing windows will be blocked and stay in the background. Since this
 * class extends from a java fx Alert it behaves as a normal dialog towards all
 * windows in the java fx thread.
 * <p>To create a custom java fx dialog one should not extend this class but rather
 * create the layout of the dialog as a fxml file, load it via the {@link FXMLLoader}
 * and set it as dialog pane through the {@link setDialogPane(DialogPane)} methode of
 * the FXAlert object.
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
                Stage fxDialogWindow = (Stage) getDialogPane().getScene().getWindow();
                fxDialogWindow.toFront();
                fxDialogWindow.requestFocus();
            });
        }

        @Override
        public void windowGainedFocus(WindowEvent e) {
            Platform.runLater(() -> {
                Stage fxDialogWindow = (Stage) getDialogPane().getScene().getWindow();
                fxDialogWindow.toFront();
                fxDialogWindow.requestFocus();
            });
        }
    };


    public FXAlert(AlertType type, String title, Image image) {
        this(type, title);
        Stage fxDialogWindow = (Stage) getDialogPane().getScene().getWindow();
        fxDialogWindow.getIcons().add(image);
    }

    public FXAlert(AlertType type, String title) {
        super(type);
        setTitle(title);
        Stage fxDialogWindow = (Stage) getDialogPane().getScene().getWindow();

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
        double mainX = JabRef.jrf.getLocationOnScreen().getX();
        double mainY = JabRef.jrf.getLocationOnScreen().getY();
        double mainWidth = JabRef.jrf.getSize().getWidth();
        double mainHeight = JabRef.jrf.getSize().getHeight();

        setX((mainX + (mainWidth / 2)) - (getWidth() / 2));
        setY((mainY + (mainHeight / 2)) - (getHeight() / 2));
    }

}
