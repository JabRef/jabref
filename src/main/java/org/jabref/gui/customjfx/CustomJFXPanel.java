package org.jabref.gui.customjfx;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.jabref.gui.AbstractView;
import org.jabref.gui.util.DefaultTaskExecutor;

/**
 * Remove as soon as possible
 */
public class CustomJFXPanel {

    public static JFXPanel wrap(Scene scene) {
        JFXPanel container = new JFXPanel();
        scene.getStylesheets().add(AbstractView.class.getResource("Main.css").toExternalForm());
        DefaultTaskExecutor.runInJavaFXThread(() -> container.setScene(scene));
        return container;
    }

}
