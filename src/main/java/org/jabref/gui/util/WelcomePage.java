package org.jabref.gui.util;

import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
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
        welcomeLabel.setFont(new Font("Arial", 40));
        welcomeLabel.setTextFill(Color.DARKSLATEGRAY);

        // Text and hyperlink for "New Library"
        Text openNewLibraryText = new Text("Open a ");
        openNewLibraryText.setFont(new Font("Arial", 22));

        Hyperlink newLibraryLink = new Hyperlink("New Library");
        newLibraryLink.setFont(new Font("Arial", 22));
        newLibraryLink.setOnAction(e -> new NewDatabaseAction(frame, preferences).execute());
        newLibraryLink.setStyle("-fx-text-fill: blue;"); // Set link color to blue
        newLibraryLink.setVisited(false); // Prevent it from changing color on click

        // Text and hyperlink for "Open Library"
        Hyperlink openLibraryLink = new Hyperlink("Existing Library");
        openLibraryLink.setFont(new Font("Arial", 22));
        openLibraryLink.setOnAction(e -> new OpenDatabaseAction(frame, preferences, aiService, dialogService, stateManager, fileUpdateMonitor,
                entryTypesManager, undoManager, clipBoardManager, taskExecutor).execute());
        openLibraryLink.setStyle("-fx-text-fill: blue;"); // Set link color to blue
        openLibraryLink.setVisited(false); // Prevent it from changing color on click

        Text orExistingDatabaseText = new Text(" or open an ");
        orExistingDatabaseText.setFont(new Font("Arial", 22));

        // TextFlows for each section
        TextFlow newLibraryFlow = new TextFlow(openNewLibraryText, newLibraryLink);
        newLibraryFlow.setTextAlignment(TextAlignment.CENTER);

        TextFlow openLibraryFlow = new TextFlow(orExistingDatabaseText, openLibraryLink);
        openLibraryFlow.setTextAlignment(TextAlignment.CENTER);

        // Add elements to the VBox
        getChildren().addAll(welcomeLabel, newLibraryFlow, openLibraryFlow);
    }
}
