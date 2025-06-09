package org.jabref.http.server.cayw.gui;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

public class SelectedItemsContainer<T> extends FlowPane {

    private final ObservableList<CAYWEntry<T>> items;

    public SelectedItemsContainer(ObservableList<CAYWEntry<T>> items) {
        this.items = items;
        setup();
    }

    public void setup() {
        this.setHgap(8);
        this.setVgap(8);
        this.setPadding(new Insets(10));

        items.forEach(this::addPill);

        items.addListener((ListChangeListener<CAYWEntry<T>>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::addPill);
                }
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(this::removePill);
                }
            }
        });
    }

    private void addPill(CAYWEntry<T> entry) {
        Pill<T> pill = new Pill<>(entry, items);
        getChildren().add(pill);
    }

    private void removePill(CAYWEntry<T> entry) {
        getChildren().removeIf(node -> {
            if (node instanceof Pill pill) {
                return pill.getEntry().equals(entry);
            }
            return false;
        });
    }

    private static class Pill<T> extends HBox {
        private final CAYWEntry<T> entry;

        public Pill(CAYWEntry<T> entry, ObservableList<CAYWEntry<T>> parentList) {
            this.entry = entry;

            this.setAlignment(Pos.CENTER_LEFT);
            this.setSpacing(5);
            this.setPadding(new Insets(5, 10, 5, 10));

            this.getStyleClass().add("pill-style");

            Button removeButton = new Button("×");
            removeButton.getStyleClass().add("pill-remove-button");

            removeButton.setOnAction(e -> {
                e.consume();
                parentList.remove(entry);
            });

            Label label = new Label(entry.getShortLabel());

            getChildren().addAll(label, removeButton);

            this.setOnMouseClicked(e -> {
                if (!e.isConsumed() && entry.getOnClick() != null) {
                    entry.getOnClick().handle(new ActionEvent(entry, null));
                }
            });
        }

        public CAYWEntry<T> getEntry() {
            return entry;
        }
    }
}
