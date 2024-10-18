package org.jabref.gui.util;

import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

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

        // Title
        Label welcomeLabel = new Label("Welcome to JabRef!");
        welcomeLabel.getStyleClass().add("welcome-label");

        // Text and hyperlink for "New Library"
        Text openNewLibraryText = new Text("Open a ");
        openNewLibraryText.getStyleClass().add("welcome-text");

        Hyperlink newLibraryLink = new Hyperlink("New Library");
        newLibraryLink.getStyleClass().add("welcome-hyperlink");
        newLibraryLink.setOnAction(e -> new NewDatabaseAction(frame, preferences).execute());

        // Text and hyperlink for "Open Library"
        Hyperlink openLibraryLink = new Hyperlink("Existing Library");
        openLibraryLink.getStyleClass().add("welcome-hyperlink");
        openLibraryLink.setOnAction(e -> new OpenDatabaseAction(frame, preferences, aiService, dialogService,
                stateManager, fileUpdateMonitor, entryTypesManager, undoManager, clipBoardManager,
                taskExecutor).execute());

        Text orExistingDatabaseText = new Text(" or open an ");
        orExistingDatabaseText.getStyleClass().add("welcome-text");

        // TextFlows for each section
        TextFlow newLibraryFlow = new TextFlow(openNewLibraryText, newLibraryLink);
        newLibraryFlow.setTextAlignment(TextAlignment.CENTER);

        TextFlow openLibraryFlow = new TextFlow(orExistingDatabaseText, openLibraryLink);
        openLibraryFlow.setTextAlignment(TextAlignment.CENTER);

        // Add elements to the VBox
        getChildren().addAll(welcomeLabel, newLibraryFlow, openLibraryFlow);
    }
}
