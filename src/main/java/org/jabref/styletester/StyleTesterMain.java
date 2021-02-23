package org.jabref.styletester;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.jabref.gui.JabRefExecutorService;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.logic.JabRefException;
import org.jabref.preferences.JabRefPreferences;

/**
 * Useful for checking the display of different controls. Not needed inside of JabRef.
 */
public class StyleTesterMain extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws JabRefException {
        StyleTesterView view = new StyleTesterView();
        JabRefPreferences preferences = JabRefPreferences.getInstance();

        DefaultFileUpdateMonitor fileUpdateMonitor = new DefaultFileUpdateMonitor();
        JabRefExecutorService.INSTANCE.executeInterruptableTask(fileUpdateMonitor, "FileUpdateMonitor");

        Scene scene = new Scene(view.getContent());
        preferences.getTheme().installCss(scene, fileUpdateMonitor);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        JabRefExecutorService.INSTANCE.shutdownEverything();
    }
}
