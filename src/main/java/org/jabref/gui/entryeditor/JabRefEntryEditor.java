package org.jabref.gui.entryeditor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class JabRefEntryEditor extends Application {

    // Main entry point for JavaFX application
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JabRef Entry Editor");

        // Create the main layout
        VBox mainLayout = new VBox(10);
//        mainLayout.setPadding(new Insets(15, 15, 15, 15));

        // Section 1: Entry Details (e.g., Title, Author)
        Label entryLabel = new Label("Edit Entry Information:");
        TextField titleField = new TextField();
        titleField.setPromptText("Enter Title");
        TextField authorField = new TextField();
        authorField.setPromptText("Enter Author");

        // Section 2: Linked Files
        Label linkedFilesLabel = new Label("Linked Files:");
        ListView<String> linkedFilesList = new ListView<>();
        linkedFilesList.getItems().addAll("File1.pdf", "File2.pdf");

        // Button to link new file
        Button linkFileButton = new Button("Link New File");
        linkFileButton.setOnAction(e -> {
            // Logic to add a new linked file (mock behavior here)
            linkedFilesList.getItems().add("NewLinkedFile.pdf");
        });

        // Section 3: Search Indexing Status
        Label searchStatusLabel = new Label("Search Index Status:");
        CheckBox reindexCheckbox = new CheckBox("Reindex linked files after editing");
        reindexCheckbox.setSelected(true); // Default is to reindex

        reindexCheckbox.setOnAction(e -> {
            if (reindexCheckbox.isSelected()) {
                System.out.println("Reindexing enabled for linked files.");
                // Add logic to trigger reindexing of linked files
            } else {
                System.out.println("Reindexing disabled for linked files.");
            }
        });

        // Button to save the entry changes
        Button saveButton = new Button("Save Entry");
        saveButton.setOnAction(e -> {
            // Here you would add logic to save the entry changes
            String title = titleField.getText();
            String author = authorField.getText();
            System.out.println("Entry saved: " + title + " by " + author);
        });

        // Add components to layout
        mainLayout.getChildren().addAll(
                entryLabel, titleField, authorField,
                linkedFilesLabel, linkedFilesList, linkFileButton,
                searchStatusLabel, reindexCheckbox,
                saveButton
        );

        // Set up the scene and stage
        Scene scene = new Scene(mainLayout, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Entry point for the program
    public static void main(String[] args) {
        launch(args);
    }
}
