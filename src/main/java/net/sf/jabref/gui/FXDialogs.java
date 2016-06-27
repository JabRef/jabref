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

import java.util.Optional;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

/**
 * This class provides static methods to create default
 * JavaFX dialogs which will also work on top of swing
 * windows. The created dialogs are instances of the
 * {@link FXAlert} class. The available dialogs in this class
 * are useful for displaying small information graphic dialogs
 * rather than complex windows. For more complex dialogs it is
 * advised to rather create a new sub class of {@link FXAlert}.
 *
 */
public abstract class FXDialogs {

    /**
     * This will create and display a new information dialog.
     * It will include a blue information icon on the left and
     * a single OK Button. To create a information dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(AlertType, String, String, ButtonType...)}
     *
     * @param title as String
     * @param content as String
     */
    public static void showInformationDialogAndWait(String title, String content) {
        FXAlert alert = createDialog(AlertType.INFORMATION, title, content);
        alert.showAndWait();
    }

    /**
     * This will create and display a new information dialog.
     * It will include a yellow warning icon on the left and
     * a single OK Button. To create a warning dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(AlertType, String, String, ButtonType...)}
     *
     * @param title as String
     * @param content as String
     */
    public static void showWarningDialogAndWait(String title, String content) {
        FXAlert alert = createDialog(AlertType.WARNING, title, content);
        alert.showAndWait();
    }

    /**
     * This will create and display a new error dialog.
     * It will include a red error icon on the left and
     * a single OK Button. To create a error dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(AlertType, String, String, ButtonType...)}
     *
     * @param title as String
     * @param content as String
     */
    public static void showErrorDialogAndWait(String title, String content) {
        FXAlert alert = createDialog(AlertType.ERROR, title, content);
        alert.showAndWait();
    }

    /**
     * This will create and display a new confirmation dialog.
     * It will include a blue question icon on the left and
     * a OK and Cancel Button. To create a confirmation dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(AlertType, String, String, ButtonType...)}
     *
     * @param title as String
     * @param content as String
     * @return Optional with the pressed Button as ButtonType
     */
    public static Optional<ButtonType> showConfirmationDialogAndWait(String title, String content) {
        FXAlert alert = createDialog(AlertType.CONFIRMATION, title, content);
        return alert.showAndWait();
    }

    /**
     * This will create and display a new dialog of the specified
     * {@link AlertType} but with user defined buttons as optional
     * {@link ButtonType}s.
     *
     * @param type as {@link AlertType}
     * @param title as String
     * @param content as String
     * @param buttonTypes
     * @return Optional with the pressed Button as ButtonType
     */
    public static Optional<ButtonType> showCustomButtonDialogAndWait(AlertType type, String title, String content,
            ButtonType... buttonTypes) {
        FXAlert alert = createDialog(type, title, content);
        alert.getButtonTypes().setAll(buttonTypes);
        return alert.showAndWait();
    }

    /**
     * This will create and display a new dialog showing a custom {@link DialogPane}
     * and using custom {@link ButtonType}s.
     *
     * @param title as String
     * @param contentPane as DialogPane
     * @param buttonTypes as ButtonType
     * @return Optional with the pressed Button as ButtonType
     */
    public static Optional<ButtonType> showCustomDialogAndWait(String title, DialogPane contentPane,
            ButtonType... buttonTypes) {
        FXAlert alert = new FXAlert(AlertType.NONE, title);
        alert.setDialogPane(contentPane);
        alert.getButtonTypes().setAll(buttonTypes);
        return alert.showAndWait();
    }

    private static FXAlert createDialog(AlertType type, String title, String content) {
        FXAlert alert = new FXAlert(type, title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert;
    }

}
