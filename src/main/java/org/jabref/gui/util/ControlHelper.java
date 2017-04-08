package org.jabref.gui.util;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ControlHelper {
    private static final Log LOGGER = LogFactory.getLog(ControlHelper.class);

    /**
     * Loads the FXML file associated to the passed control.
     * The FMXL file should have the same name as the control with ending ".fxml" appended
     */
    public static void loadFXMLForControl(Node control) {
        Class<?> clazz = control.getClass();
        String clazzName = clazz.getSimpleName();

        FXMLLoader fxmlLoader = new FXMLLoader(clazz.getResource(clazzName + ".fxml"));
        fxmlLoader.setController(control);
        fxmlLoader.setRoot(control);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            LOGGER.error(exception);
        }
    }
}
