package org.jabref.gui.walkthrough.effects;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.jabref.gui.testutils.JavaFxExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(JavaFxExtension.class)
class SpotlightTest {

    /// A spotlight whose overlay was hidden (its target became invisible) must still be able to move on to the next
    /// target instead of failing on "already attached".
    @Test
    void hiddenSpotlightMovesToNextTarget() {
        JavaFxExtension.invokeAndWait(() -> {
            Button first = new Button("first");
            Button second = new Button("second");
            Pane overlay = new Pane();
            StackPane root = new StackPane(first, second, overlay);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 300, 200));
            stage.show();
            root.layout();

            Spotlight spotlight = new Spotlight(overlay);
            spotlight.attach(first);
            first.setVisible(false);
            spotlight.updateLayout();
            assertTrue(overlay.getChildren().stream().noneMatch(javafx.scene.Node::isVisible));

            assertDoesNotThrow(() -> spotlight.transitionTo(second));
            assertTrue(overlay.getChildren().stream().anyMatch(javafx.scene.Node::isVisible));
        });
    }

    @Test
    void hiddenPingMovesToNextTarget() {
        JavaFxExtension.invokeAndWait(() -> {
            Button first = new Button("first");
            Button second = new Button("second");
            Pane overlay = new Pane();
            StackPane root = new StackPane(first, second, overlay);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 300, 200));
            stage.show();
            root.layout();

            Ping ping = new Ping(overlay);
            ping.attach(first);
            first.setVisible(false);
            ping.updateLayout();

            assertDoesNotThrow(() -> ping.transitionTo(second));
        });
    }
}
