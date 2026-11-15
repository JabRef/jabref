package org.jabref.gui.util.component;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.jabref.gui.testutils.JavaFxTest;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class ListScrollPaneTest extends JavaFxTest {
    private ListScrollPane<String> scrollPane;

    @Override
    public void start(Stage stage) {
        scrollPane = new ListScrollPane<>();
        scrollPane.setRenderer(_ -> {
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

    @Test
    void switchingAgainBeforeRestoreKeepsSavedPosition() {
        ObservableList<String> first = FXCollections.observableArrayList("a", "b", "c", "d", "e");
        ObservableList<String> second = FXCollections.observableArrayList("x", "y", "z");

        interact(() -> scrollPane.setItems(first));
        awaitEvents();
        interact(() -> scrollPane.setVvalue(0.4));
        interact(() -> scrollPane.setItems(second));
        awaitEvents();

        // Switch back and away again without letting the deferred restore run
        interact(() -> {
            scrollPane.setItems(first);
            scrollPane.setItems(second);
        });
        awaitEvents();
        assertEquals(1.0, scrollPane.getVvalue(), 0.001);

        interact(() -> scrollPane.setItems(first));
        awaitEvents();
        assertEquals(0.4, scrollPane.getVvalue(), 0.001);
    }

    @Test
    void renderedNodesFollowItemChangesInDisplayOrder() {
        interact(() -> {
            scrollPane.setRenderer(Text::new);
            ObservableList<Node> rendered = scrollPane.getRenderedNodes();
            ObservableList<String> items = FXCollections.observableArrayList("first", "second");
            scrollPane.setItems(items);
            assertEquals(List.of("first", "second"), rendered.stream().map(node -> ((Text) node).getText()).toList());

            items.set(0, "replacement");
            items.add("third");
            FXCollections.reverse(items);
            assertEquals(List.of("third", "second", "replacement"), rendered.stream().map(node -> ((Text) node).getText()).toList());

            items.removeFirst();
            assertEquals(List.of("second", "replacement"), rendered.stream().map(node -> ((Text) node).getText()).toList());
        });
    }

    @Test
    void renderedNodesCannotBeModifiedByCaller() {
        interact(() -> {
            scrollPane.setItems(FXCollections.observableArrayList("first"));
            assertThrows(UnsupportedOperationException.class, () -> scrollPane.getRenderedNodes().clear());
            assertEquals(1, scrollPane.getRenderedNodes().size());
        });
    }
}
