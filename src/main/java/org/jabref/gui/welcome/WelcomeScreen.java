package org.jabref.gui.welcome;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class WelcomeScreen {

    public static Scene createWelcomeScene() {
        // Create the layout
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);

        // Welcome label with larger font and color
        Label welcomeLabel = new Label("Welcome to JabRef!");
        welcomeLabel.setFont(new Font("Arial", 28));
        welcomeLabel.setTextFill(Color.DARKSLATEGRAY);

        // Create buttons with customized styling
        Button openButton = createStyledButton("Open Library");
        Button createButton = createStyledButton("Create New Library");

        // Add elements to the layout
        layout.getChildren().addAll(welcomeLabel, openButton, createButton);

        // Create and return the scene
        return new Scene(layout, 800, 600);
    }

    // Helper method to create styled buttons
    private static Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setFont(new Font("Arial", 16));

        // Primary button color: #50618F, with slight padding
        button.setStyle("-fx-background-color: #50618F; -fx-text-fill: white; "
                + "-fx-background-radius: 10; -fx-padding: 10 20 10 20;");

        // Darken slightly on hover
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #405070; -fx-text-fill: white; "
                        + "-fx-background-radius: 10; -fx-padding: 10 20 10 20;"
        ));

        // Reset to original color on exit
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: #50618F; -fx-text-fill: white; "
                        + "-fx-background-radius: 10; -fx-padding: 10 20 10 20;"
        ));

        return button;
    }
}
