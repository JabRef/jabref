package org.jabref.gui.util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * A simple Welcome Page for the JabRef application.
 */
public class WelcomePage extends VBox {

    public WelcomePage() {
        setAlignment(Pos.CENTER);
        setSpacing(10);

        Label welcomeLabel = new Label("Welcome to JabRef!");
        welcomeLabel.setFont(new Font("Arial", 28));
        welcomeLabel.setTextFill(Color.DARKSLATEGRAY);

        Label instructions = new Label("Click 'New Library' to get started or open an existing database.");
        instructions.setFont(new Font("Arial", 16));
        instructions.setTextFill(Color.GRAY);

        getChildren().addAll(welcomeLabel, instructions);
    }
}
