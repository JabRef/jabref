package org.jabref.gui.util.component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import org.jabref.gui.testutils.JavaFxTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListScrollPaneTest extends JavaFxTest {
    private ListScrollPane<String> scrollPane;

    @Override
    public void start(Stage stage) {
        scrollPane = new ListScrollPane<>();
        scrollPane.setRenderer(item -> {
            Region region = new Region();
            region.setPrefHeight(100);
            return region;
        });
        scrollPane.setAutoScrollToBottom(true);
        stage.setScene(new Scene(scrollPane, 200, 200));
        stage.show();
    }

    @Test
    void switchingBackRestoresScrollPosition() {
        ObservableList<String> first = FXCollections.observableArrayList("a", "b", "c", "d", "e");
        ObservableList<String> second = FXCollections.observableArrayList("x", "y", "z");

        interact(() -> scrollPane.setItems(first));
        awaitEvents();
        interact(() -> scrollPane.setVvalue(0.4));

        interact(() -> scrollPane.setItems(second));
        awaitEvents();
        assertEquals(1.0, scrollPane.getVvalue(), 0.001);

        interact(() -> scrollPane.setItems(first));
        awaitEvents();
        assertEquals(0.4, scrollPane.getVvalue(), 0.001);
    }
}
