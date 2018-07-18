package org.jabref.styletester;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.jabref.gui.util.ThemeLoader;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

/**
 * Useful for checking the display of different controls. Not needed inside of JabRef.
 */
public class StyleTesterMain extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        StyleTesterView view = new StyleTesterView();

        Scene scene = new Scene(view.getContent());
        new ThemeLoader(new DummyFileUpdateMonitor()).installBaseCss(scene, JabRefPreferences.getInstance());
        stage.setScene(scene);
        stage.show();
    }
}
