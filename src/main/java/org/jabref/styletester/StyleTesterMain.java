package org.jabref.styletester;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.jabref.JabRefExecutorService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.gui.util.ThemeLoader;
import org.jabref.preferences.JabRefPreferences;

/**
 * Useful for checking the display of different controls. Not needed inside of JabRef.
 */
public class StyleTesterMain extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        StyleTesterView view = new StyleTesterView();

        IconTheme.loadFonts();

        DefaultFileUpdateMonitor fileUpdateMonitor = new DefaultFileUpdateMonitor();
        JabRefExecutorService.INSTANCE.executeInterruptableTask(fileUpdateMonitor, "FileUpdateMonitor");

        Scene scene = new Scene(view.getContent());
        new ThemeLoader(fileUpdateMonitor).installBaseCss(scene, JabRefPreferences.getInstance());
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        JabRefExecutorService.INSTANCE.shutdownEverything();
    }
}
