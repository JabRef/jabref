package org.jabref.gui.util;

import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.frame.JabRefFrame;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;

/**
 * A simple Welcome Page for the JabRef application.
 */
public class WelcomePage extends VBox {

    public WelcomePage(JabRefFrame frame, GuiPreferences preferences) {
        setAlignment(Pos.CENTER);
        setSpacing(10);

        Label welcomeLabel = new Label("Welcome to JabRef!");
        welcomeLabel.setFont(new Font("Arial", 28));
        welcomeLabel.setTextFill(Color.DARKSLATEGRAY);

        Text normalText = new Text("Open a ");
        normalText.setFont(new Font("Arial", 16));
        normalText.setFill(Color.GRAY);

        // Create a hyperlink for "New Library"
        Hyperlink newLibraryLink = new Hyperlink("New Library");
        newLibraryLink.setFont(new Font("Arial", 16));
        newLibraryLink.setOnAction(e -> {
            // Trigger NewDatabaseAction using frame and preferences
            new NewDatabaseAction(frame, preferences).execute();
        });

        // Create a TextFlow to combine normal text and the hyperlink
        TextFlow textFlow = new TextFlow(normalText, newLibraryLink);
        textFlow.setTextAlignment(javafx.scene.text.TextAlignment.CENTER); // Center the text within TextFlow
        textFlow.setMaxWidth(Double.MAX_VALUE);
        setAlignment(Pos.CENTER);  // Center align VBox contents

        Label instructions = new Label("or open an existing database.");
        instructions.setFont(new Font("Arial", 16));
        instructions.setTextFill(Color.GRAY);

        getChildren().addAll(welcomeLabel, textFlow, instructions);
    }
}
