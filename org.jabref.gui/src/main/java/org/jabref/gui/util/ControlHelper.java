package org.jabref.gui.util;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import org.jabref.gui.AbstractView;
import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlHelper.class);

    /**
     * Loads the FXML file associated to the passed control.
     * The FMXL file should have the same name as the control with ending ".fxml" appended
     */
    public static void loadFXMLForControl(Parent control) {
        Class<?> clazz = control.getClass();
        String clazzName = clazz.getSimpleName();

        FXMLLoader fxmlLoader = new FXMLLoader(clazz.getResource(clazzName + ".fxml"), Localization.getMessages());
        fxmlLoader.setController(control);
        fxmlLoader.setRoot(control);
        try {
            fxmlLoader.load();

            // Add our base css file
            control.getStylesheets().add(0, AbstractView.class.getResource("Main.css").toExternalForm());

            // Add language resource

        } catch (IOException exception) {
            LOGGER.error("Problem loading fxml for control", exception);
        }
    }
}
