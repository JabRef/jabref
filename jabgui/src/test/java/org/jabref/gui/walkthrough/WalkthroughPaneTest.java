package org.jabref.gui.walkthrough;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;

import org.jabref.gui.testutils.JavaFxExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void emptyPaneDoesNotSwallowInput() {
        WalkthroughPane pane = new WalkthroughPane();

        assertTrue(pane.isMouseTransparent());

        JavaFxExtension.invokeAndWait(() -> pane.getChildren().add(new Region()));

        assertFalse(pane.isMouseTransparent());
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
