package org.jabref.http.server.cayw.gui;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

public class SelectedItemsContainer extends FlowPane {

    private final ObservableList<CAYWEntry> items;

    public SelectedItemsContainer(ObservableList<CAYWEntry> items) {
        this.items = items;
        setup();
    }

    private void setup() {
        this.setHgap(8);
        this.setVgap(8);
        this.setPadding(new Insets(10));

        items.forEach(this::addChip);

        items.addListener((ListChangeListener<CAYWEntry>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::addChip);
                }
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(this::removeChip);
                }
            }
        });
    }

    private void addChip(CAYWEntry entry) {
        Chip chip = new Chip(entry, items);
        getChildren().add(chip);
    }

    private void removeChip(CAYWEntry entry) {
        getChildren().removeIf(node -> {
            if (node instanceof SelectedItemsContainer.Chip chip) {
                return chip.getEntry().equals(entry);
            }
            return false;
        });
    }

    private static class Chip extends HBox {
        private final CAYWEntry entry;

        public Chip(CAYWEntry entry, ObservableList<CAYWEntry> parentList) {
            this.entry = entry;

            this.setAlignment(Pos.CENTER_LEFT);
            this.setSpacing(5);
            this.setPadding(new Insets(5, 10, 5, 10));

            this.getStyleClass().add("chip-style");

            Button removeButton = new Button("×");
            removeButton.getStyleClass().add("chip-remove-button");

            removeButton.setOnAction(e -> {
                e.consume();
                parentList.remove(entry);
            });

            Label label = new Label(entry.shortLabel());

            getChildren().addAll(label, removeButton);

            this.setOnMouseClicked(e -> {
                // TODO: Handle the click event and show custom fields like prefix/suffix etc.
            });
        }

        public CAYWEntry getEntry() {
            return entry;
        }
    }
}
