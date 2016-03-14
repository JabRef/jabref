package net.sf.jabref;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * JabRef MainClass
 */
public class JabRefMain extends Application {

    private static String[] arguments;


    public static void main(String[] args) {
        arguments = args;
        launch(arguments);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        new JabRef().start(arguments);
        // the next line is essential when working with swing and java fx threads simultaniously
        Platform.setImplicitExit(false);
    }

}
