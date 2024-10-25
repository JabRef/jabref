package org.jabref.gui.util;

import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.frame.JabRefFrame;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

/**
 * A simple Welcome Page for the JabRef application.
 */
public class WelcomePage extends VBox {

    public WelcomePage(JabRefFrame frame,
                       GuiPreferences preferences,
                       AiService aiService,
                       DialogService dialogService,
                       StateManager stateManager,
                       FileUpdateMonitor fileUpdateMonitor,
                       BibEntryTypesManager entryTypesManager,
                       CountingUndoManager undoManager,
                       ClipBoardManager clipBoardManager,
                       TaskExecutor taskExecutor) {

        setAlignment(Pos.CENTER);
        setSpacing(10);

        Label welcomeLabel = new Label("Welcome to JabRef!");
        welcomeLabel.getStyleClass().add("welcome-label");

        Hyperlink newLibrary = new Hyperlink("Open a New Library");
        newLibrary.getStyleClass().add("welcome-hyperlink");
        newLibrary.setOnAction(e -> new NewDatabaseAction(frame, preferences).execute());

        Hyperlink openLibrary = new Hyperlink("or open an Existing Library");
        openLibrary.getStyleClass().add("welcome-hyperlink");
        openLibrary.setOnAction(e -> new OpenDatabaseAction(frame, preferences, aiService, dialogService,
                stateManager, fileUpdateMonitor, entryTypesManager, undoManager, clipBoardManager,
                taskExecutor).execute());

        getChildren().addAll(welcomeLabel, newLibrary, openLibrary);
    }
}
