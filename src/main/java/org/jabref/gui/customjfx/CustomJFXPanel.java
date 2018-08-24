package org.jabref.gui.customjfx;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.jabref.Globals;
import org.jabref.gui.util.DefaultTaskExecutor;

/**
 * TODO: Remove as soon as possible
 */
public class CustomJFXPanel {

    public static JFXPanel wrap(Scene scene) {
        JFXPanel container = new JFXPanel();
        Globals.getThemeLoader().installBaseCss(scene, Globals.prefs);
        DefaultTaskExecutor.runInJavaFXThread(() -> container.setScene(scene));
        return container;
    }

}
