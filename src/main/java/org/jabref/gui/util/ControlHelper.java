package org.jabref.gui.util;

import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.Globals;
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
            if (Globals.getThemeLoader() != null) {
                Globals.getThemeLoader().installBaseCss(control);
            }

        } catch (IOException exception) {
            LOGGER.error("Problem loading fxml for control", exception);
        }
    }

    public static void setAction(ButtonType buttonType, DialogPane dialogPane, Consumer<Event> consumer) {
        Button button = (Button) dialogPane.lookupButton(buttonType);
        button.addEventFilter(ActionEvent.ACTION, (event -> {
            consumer.accept(event);
            event.consume();
        }));
    }

    public static void setSwingContent(DialogPane dialogPane, JComponent content) {
        SwingNode node = new SwingNode();
        SwingUtilities.invokeLater(() -> node.setContent(content));
        node.setVisible(true);

        dialogPane.setContent(node);
    }
}
