package org.jabref.gui;

import java.util.List;

import javafx.application.Application;
import javafx.stage.Stage;

import org.jabref.gui.openoffice.OOBibBaseConnect;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

/**
 * JabRef's main class to process command line options and to start the UI
 */
public class MainApplication extends Application {
    private static List<ParserResult> parserResults;
    private static boolean isBlank;
    private static JabRefPreferences preferences;
    private static FileUpdateMonitor fileUpdateMonitor;

    public static void main(List<ParserResult> parserResults,
                            boolean blank,
                            JabRefPreferences preferences,
                            FileUpdateMonitor fileUpdateMonitor,
                            String[] args) {
        MainApplication.parserResults = parserResults;
        MainApplication.isBlank = blank;
        MainApplication.preferences = preferences;
        MainApplication.fileUpdateMonitor = fileUpdateMonitor;
        launch(args);
    }

    @Override
    public void start(Stage mainStage) {
        FallbackExceptionHandler.installExceptionHandler();
        Globals.startBackgroundTasks();
        new JabRefGUI(mainStage, parserResults, isBlank, preferences, fileUpdateMonitor);
    }

    @Override
    public void stop() {
        OOBibBaseConnect.closeOfficeConnection();
        Globals.stopBackgroundTasks();
        Globals.shutdownThreadPools();
    }
}
