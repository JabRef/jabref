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
    void paneInstalledInAParentIsFoundByItsWindow() {
        StackPane content = new StackPane();
        AtomicReference<WalkthroughPane> pane = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> pane.set(WalkthroughPane.installIn(content)));

        Stage stage = showStage(new Scene(content));

        assertEquals(Optional.of(pane.get()), WalkthroughPane.of(stage));
    }

    @Test
    void installingInTheSameParentTwiceKeepsTheFirstPane() {
        StackPane content = new StackPane();

        AtomicReference<WalkthroughPane> first = new AtomicReference<>();
        AtomicReference<WalkthroughPane> second = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> {
            first.set(WalkthroughPane.installIn(content));
            second.set(WalkthroughPane.installIn(content));
        });

        assertSame(first.get(), second.get());
        assertEquals(1, content.getChildren().stream().filter(WalkthroughPane.class::isInstance).count());
    }

    /// The pane covers the window, so its size comes from the scene rather than from whichever parent
    /// happens to hold it.
    @Test
    void paneCoversTheWholeScene() {
        StackPane content = new StackPane();
        AtomicReference<WalkthroughPane> pane = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> pane.set(WalkthroughPane.installIn(content)));

        showStage(new Scene(content, 640, 480));

        assertEquals(640, pane.get().getWidth());
        assertEquals(480, pane.get().getHeight());
    }

    @Test
    void obtainingThePaneNeverReplacesTheSceneRoot() {
        StackPane content = new StackPane();
        Scene scene = new Scene(content);
        Stage stage = showStage(scene);
        Parent rootBefore = scene.getRoot();

        JavaFxExtension.invokeAndWait(() -> WalkthroughPane.ensureFor(stage));

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
            pane.set(WalkthroughPane.installIn(newDialog.getDialogPane()));
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
    void popupWindowGetsAPaneOnDemand() {
        Stage owner = showStage(new Scene(new StackPane()));
        Popup popup = new Popup();

        AtomicReference<Optional<WalkthroughPane>> pane = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> {
            popup.getContent().add(new Region());
            popup.show(owner);
            pane.set(WalkthroughPane.ensureFor(popup));
        });

        assertTrue(pane.get().isPresent());
        assertEquals(pane.get(), WalkthroughPane.of(popup));

        JavaFxExtension.invokeAndWait(popup::hide);
    }

    @Test
    void emptyPaneDoesNotSwallowInput() {
        StackPane content = new StackPane();
        AtomicReference<WalkthroughPane> pane = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> pane.set(WalkthroughPane.installIn(content)));

        assertTrue(pane.get().isMouseTransparent());

        JavaFxExtension.invokeAndWait(() -> pane.get().getChildren().add(new Region()));

        assertFalse(pane.get().isMouseTransparent());
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
