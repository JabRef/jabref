package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.icore.ConferenceRankingEntry;
import org.jabref.logic.icore.ICoreRankingRepository;
import org.jabref.logic.util.ConferenceUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class ICoreRankingEditor extends HBox implements FieldEditorFX {

    private final Field field;
    private final TextField textField;
    private BibEntry currentEntry;
    private final ICoreRankingRepository repo;

    public ICoreRankingEditor(Field field) {
        this.field = field;
        this.textField = new TextField();
        this.repo = new ICoreRankingRepository(); // Load once

        this.textField.setPromptText("Enter or lookup ICORE rank");

        Button lookupButton = new Button("Lookup Rank");
        lookupButton.setOnAction(event -> lookupRank());

        this.getChildren().addAll(textField, lookupButton);
        this.setSpacing(10);
    }

    // Deprecated method it only shows rank by puttin the acronym in the text field
//    private void lookupRank() {
//        if (currentEntry == null) {
////            System.out.println("No entry is currently bound.");
//            return;
//        }
//
//        // 1. Try icoreranking field first
//        Optional<String> icoreField = currentEntry.getField(StandardField.ICORANKING);
//        if (icoreField.isPresent()) {
//            String raw = icoreField.get();
////            System.out.println("Using ICORE field: " + raw);
//
//            Optional<String> acronymRank = repo.getRankingFor(raw.toLowerCase());
//            String result = acronymRank.orElse("Not ranked");
//            textField.setText(result);
////            System.out.println("Lookup result from ICORE: " + result);
//            return;
//        }
//
//        // 2. Then fallback to booktitle
//        Optional<String> bookTitle = currentEntry.getFieldOrAlias(StandardField.BOOKTITLE);
//        // 3. Then fallback to journal
//        if (bookTitle.isEmpty()) {
//            bookTitle = currentEntry.getField(StandardField.JOURNAL);
//        }
//
//        if (bookTitle.isEmpty()) {
////            System.out.println("No usable field found.");
//            textField.setText("Not ranked");
//            return;
//        }
//
//        String rawInput = bookTitle.get();
////        System.out.println("Detected Title: '" + rawInput + "'");
//
//        Optional<String> acronym = ConferenceUtil.extractAcronym(rawInput);
////        acronym.ifPresent(acr -> System.out.println("Extracted acronym: " + acr));
//
//        Optional<String> rank = acronym.flatMap(repo::getRankingFor)
//                .or(() -> repo.getRankingFor(rawInput));
//
//        String result = rank.orElse("Not ranked");
//        textField.setText(result);
////        System.out.println("Lookup result: " + result);
//    }

// By providing the acronym fetches the data from csv
private void lookupRank() {
    if (currentEntry == null) {
        return;
    }

    Optional<String> icoreField = currentEntry.getField(StandardField.ICORANKING);
    Optional<String> bookTitle = currentEntry.getFieldOrAlias(StandardField.BOOKTITLE);
    if (bookTitle.isEmpty()) {
        bookTitle = currentEntry.getField(StandardField.JOURNAL);
    }

    Optional<String> finalBookTitle = bookTitle;
    String rawInput = icoreField.orElseGet(() -> finalBookTitle.orElse("Unknown"));

    Optional<String> acronym = ConferenceUtil.extractAcronym(rawInput); // Extracting the acronym from our input field
    Optional<ConferenceRankingEntry> result = acronym.flatMap(repo::getFullEntry)
            .or(() -> repo.getFullEntry(rawInput));  // Finding if any matching entry present in csv file
// If present then print the info

    if (result.isPresent()) {
        ConferenceRankingEntry entry = result.get();

        // Show in new dialog
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("ICORE Ranking Info");
        alert.setHeaderText("Found Conference Details");
        alert.setContentText(entry.toString());
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(600, 400);
        alert.showAndWait();

        textField.setText(entry.rank); // still show rank in the field
    } else {
        textField.setText("Not ranked");
    }
}

    @Override
    public void bindToEntry(BibEntry entry) {
        this.currentEntry = entry;

//        System.out.println("ENTRY booktitle = " + entry.getField(StandardField.BOOKTITLE).orElse("none"));
//        System.out.println("ENTRY journal = " + entry.getField(StandardField.JOURNAL).orElse("none"));
//        System.out.println("ENTRY title = " + entry.getField(StandardField.TITLE).orElse("none"));

        entry.getField(field).ifPresent(textField::setText);

        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            entry.setField(field, newVal);
        });
    }

    @Override
    public void establishBinding(TextInputControl textInputControl, StringProperty viewModelTextProperty,
                                 KeyBindingRepository keyBindingRepository, UndoAction undoAction, RedoAction redoAction) {
        FieldEditorFX.super.establishBinding(textInputControl, viewModelTextProperty, keyBindingRepository, undoAction, redoAction);
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @Override
    public void focus() {
        FieldEditorFX.super.focus();
    }

    @Override
    public double getWeight() {
        return FieldEditorFX.super.getWeight();
    }

    @Override
    public void requestFocus() {
        textField.requestFocus();
    }

    @Override
    public Node getStyleableNode() {
        return super.getStyleableNode();
    }
}
