package org.jabref.gui.walkthrough.utils;

import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.testutils.JavaFxExtension;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class WalkthroughScrollerTest {

    @Test
    void scrollsTargetIntoViewWhenCreated() {
        AtomicReference<ScrollPane> scrollPane = new AtomicReference<>();

        JavaFxExtension.invokeAndWait(() -> {
            Region spacer = new Region();
            spacer.setPrefHeight(400);
            Region target = new Region();
            target.setPrefHeight(40);
            VBox content = new VBox(spacer, target);
            ScrollPane newScrollPane = new ScrollPane(content);
            Stage stage = new Stage();
            stage.setScene(new Scene(newScrollPane, 200, 100));
            stage.show();
            newScrollPane.setVvalue(0);

            new WalkthroughScroller(target);
            scrollPane.set(newScrollPane);
        });

        assertEquals(1, scrollPane.get().getVvalue());
    }
}
