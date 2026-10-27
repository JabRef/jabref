package org.jabref.gui.walkthrough;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Popup;
import javafx.stage.Stage;

import org.jabref.gui.testutils.JavaFxExtension;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class WalkthroughPaneTest {

    @Test
    void paneAddedToAWindowIsFoundByThatWindow() {
        WalkthroughPane pane = new WalkthroughPane();
        StackPane content = new StackPane(pane);

        Stage stage = showStage(new Scene(content));

        assertEquals(Optional.of(pane), WalkthroughPane.of(stage));
    }

    /// The pane covers the window, so its size comes from the scene rather than from whichever parent
    /// happens to hold it.
    @Test
    void paneCoversTheWholeScene() {
        WalkthroughPane pane = new WalkthroughPane();
        StackPane content = new StackPane(pane);

        showStage(new Scene(content, 640, 480));

        assertEquals(640, pane.getWidth());
        assertEquals(480, pane.getHeight());
    }

    @Test
    void lookingUpAPaneNeverReplacesTheSceneRoot() {
        StackPane content = new StackPane();
        Scene scene = new Scene(content);
        Stage stage = showStage(scene);
        Parent rootBefore = scene.getRoot();

        JavaFxExtension.invokeAndWait(() -> WalkthroughPane.of(stage));

        assertSame(rootBefore, scene.getRoot());
    }

    /// A dialog reassigns its scene root on every show and swaps in a placeholder on close, so a pane that
    /// survives a show/hide cycle has to belong to the dialog pane rather than to the scene.
    @Test
    void paneSurvivesADialogBeingShownAgain() {
        AtomicReference<Dialog<Void>> dialog = new AtomicReference<>();
        AtomicReference<WalkthroughPane> pane = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> {
            Dialog<Void> newDialog = new Dialog<>();
            newDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            pane.set(new WalkthroughPane());
            newDialog.getDialogPane().getChildren().add(pane.get());
            newDialog.show();
            newDialog.close();
            newDialog.show();
            dialog.set(newDialog);
        });

        DialogPane dialogPane = dialog.get().getDialogPane();
        assertTrue(dialogPane.getChildren().contains(pane.get()));
        assertEquals(Optional.of(pane.get()), WalkthroughPane.of(dialogPane.getScene().getWindow()));

        JavaFxExtension.invokeAndWait(() -> dialog.get().close());
    }

    /// Windows JabRef does not build itself -- context menus and other popups -- get their pane the first
    /// time a walkthrough needs one.
    @Test
    void popupGetsItsPaneOnFirstLookup() {
        Stage owner = showStage(new Scene(new StackPane()));
        Popup popup = new Popup();

        AtomicReference<Optional<WalkthroughPane>> pane = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> {
            popup.getContent().add(new Region());
            popup.show(owner);
            pane.set(WalkthroughPane.of(popup));
        });

        assertTrue(pane.get().isPresent());
        assertEquals(pane.get(), WalkthroughPane.of(popup));

        JavaFxExtension.invokeAndWait(popup::hide);
    }

    /// A popup window is sized from its content, and a context menu's drop shadow offsets that content. A pane
    /// sized to the popup's scene would enlarge the very window it measures itself against, until the bounds
    /// computation overflows the stack and the menu no longer renders.
    @Test
    void contextMenuKeepsItsSizeOnceItHasAPane() {
        Stage owner = showStage(new Scene(new StackPane(), 400, 300));
        ContextMenu menu = new ContextMenu(new MenuItem("Open library"), new MenuItem("Preferences"));
        JavaFxExtension.invokeAndWait(() -> {
            menu.show(owner, owner.getX() + 20, owner.getY() + 20);
            menu.getScene().getRoot().lookup(".context-menu").setEffect(new DropShadow(12, Color.BLACK));
        });
        JavaFxExtension.awaitEvents();
        double width = menu.getWidth();
        double height = menu.getHeight();

        JavaFxExtension.invokeAndWait(() -> WalkthroughPane.of(menu));
        JavaFxExtension.awaitEvents();
        JavaFxExtension.invokeAndWait(() -> {
            menu.hide();
            menu.show(owner, owner.getX() + 20, owner.getY() + 20);
        });
        JavaFxExtension.awaitEvents();

        assertEquals(width, menu.getWidth());
        assertEquals(height, menu.getHeight());

        JavaFxExtension.invokeAndWait(menu::hide);
    }

    /// The pane covers the window in front of its content, so a click next to an overlay node -- through a
    /// spotlight's hole, beside a panel -- has to reach the control a walkthrough step is waiting for.
    @Test
    void clickNextToAnOverlayNodeReachesTheWindowContent() {
        AtomicBoolean contentClicked = new AtomicBoolean();
        Button content = new Button("Content");
        content.setOnAction(_ -> contentClicked.set(true));
        WalkthroughPane pane = new WalkthroughPane();
        showStage(new Scene(new StackPane(content, pane), 300, 200));

        JavaFxExtension.invokeAndWait(() -> {
            Region overlayNode = new Region();
            overlayNode.setMaxSize(20, 20);
            StackPane.setAlignment(overlayNode, Pos.TOP_LEFT);
            pane.getChildren().add(overlayNode);
        });
        click(content);

        assertTrue(contentClicked.get());
    }

    @Test
    void overlayNodeReceivesClicks() {
        AtomicBoolean overlayClicked = new AtomicBoolean();
        Button overlayButton = new Button("Quit");
        overlayButton.setOnAction(_ -> overlayClicked.set(true));
        WalkthroughPane pane = new WalkthroughPane();
        showStage(new Scene(new StackPane(new Button("Content"), pane), 300, 200));

        JavaFxExtension.invokeAndWait(() -> {
            StackPane.setAlignment(overlayButton, Pos.TOP_LEFT);
            pane.getChildren().add(overlayButton);
        });
        click(overlayButton);

        assertTrue(overlayClicked.get());
    }

    private static void click(Node node) {
        JavaFxExtension.invokeAndWait(() -> {
            // Overlay nodes are added to a live window; lay them out now instead of waiting for the next pulse.
            Parent root = node.getScene().getRoot();
            root.applyCss();
            root.layout();
            Robot robot = new Robot();
            Bounds bounds = node.localToScreen(node.getBoundsInLocal());
            robot.mouseMove(bounds.getCenterX(), bounds.getCenterY());
            robot.mouseClick(MouseButton.PRIMARY);
        });
        JavaFxExtension.awaitEvents();
    }

    private static Stage showStage(Scene scene) {
        AtomicReference<Stage> stage = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> {
            Stage newStage = new Stage();
            newStage.setScene(scene);
            newStage.show();
            stage.set(newStage);
        });
        return stage.get();
    }
}
