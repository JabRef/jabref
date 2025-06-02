package org.jabref.gui.walkthrough;

import java.util.Optional;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.jabref.gui.util.component.PulseAnimateIndicator;
import org.jabref.gui.walkthrough.declarative.WalkthroughStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Display a walkthrough overlay on top of the main application window.
 */
public class WalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughOverlay.class);
    private final Stage parentStage;
    private final Walkthrough manager;
    private final GridPane overlayPane;
    private final PulseAnimateIndicator pulseIndicator;
    private final Pane originalRoot;
    private final StackPane stackContainer;

    public WalkthroughOverlay(Stage stage, Walkthrough manager) {
        this.parentStage = stage;
        this.manager = manager;

        overlayPane = new GridPane();
        overlayPane.setStyle("-fx-background-color: transparent;");
        overlayPane.setVisible(false);
        overlayPane.setMaxWidth(Double.MAX_VALUE);
        overlayPane.setMaxHeight(Double.MAX_VALUE);

        Scene scene = stage.getScene();
        if (scene == null) {
            LOGGER.error("Parent stage's scene must not be null to initialize WalkthroughOverlay.");
            throw new IllegalStateException("Parent stage's scene must not be null");
        }

        originalRoot = (Pane) scene.getRoot();
        stackContainer = new StackPane();

        stackContainer.getChildren().add(originalRoot);
        stackContainer.getChildren().add(overlayPane);
        pulseIndicator = new PulseAnimateIndicator(stackContainer);

        scene.setRoot(stackContainer);
    }

    public void displayStep(WalkthroughStep step) {
        if (step == null) {
            hide();
            return;
        }

        show();

        pulseIndicator.stop();
        overlayPane.getChildren().clear();

        switch (step.stepType()) {
            case FULL_SCREEN:
                displayFullScreenStep(step);
                break;
            case LEFT_PANEL:
                displayPanelStep(step, Pos.CENTER_LEFT);
                break;
            case RIGHT_PANEL:
                displayPanelStep(step, Pos.CENTER_RIGHT);
                break;
            case TOP_PANEL:
                displayPanelStep(step, Pos.TOP_CENTER);
                break;
            case BOTTOM_PANEL:
                displayPanelStep(step, Pos.BOTTOM_CENTER);
                break;
        }
    }

    private void displayFullScreenStep(WalkthroughStep step) {
        Node fullScreenContent = WalkthroughUIFactory.createFullscreen(step, manager);
        overlayPane.getChildren().clear();
        overlayPane.getChildren().add(fullScreenContent);

        overlayPane.getRowConstraints().clear();
        overlayPane.getColumnConstraints().clear();
        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setVgrow(Priority.ALWAYS);
        overlayPane.getRowConstraints().add(rowConstraints);
        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHgrow(Priority.ALWAYS);
        overlayPane.getColumnConstraints().add(columnConstraints);

        GridPane.setHgrow(fullScreenContent, Priority.ALWAYS);
        GridPane.setVgrow(fullScreenContent, Priority.ALWAYS);
        GridPane.setFillWidth(fullScreenContent, true);
        GridPane.setFillHeight(fullScreenContent, true);

        overlayPane.setAlignment(Pos.CENTER);
        overlayPane.setVisible(true);
    }

    private void displayPanelStep(WalkthroughStep step, Pos position) {
        Node panelContent = WalkthroughUIFactory.createSidePanel(step, manager);
        overlayPane.getChildren().clear();
        overlayPane.getChildren().add(panelContent);
        panelContent.setMouseTransparent(false);

        overlayPane.getRowConstraints().clear();
        overlayPane.getColumnConstraints().clear();

        GridPane.setHgrow(panelContent, Priority.NEVER);
        GridPane.setVgrow(panelContent, Priority.NEVER);
        GridPane.setFillWidth(panelContent, false);
        GridPane.setFillHeight(panelContent, false);

        RowConstraints rowConstraints = new RowConstraints();
        ColumnConstraints columnConstraints = new ColumnConstraints();

        overlayPane.setAlignment(position);

        switch (position) {
            case CENTER_LEFT:
            case CENTER_RIGHT:
                rowConstraints.setVgrow(Priority.ALWAYS);
                columnConstraints.setHgrow(Priority.NEVER);
                GridPane.setFillHeight(panelContent, true);
                break;
            case TOP_CENTER:
            case BOTTOM_CENTER:
                columnConstraints.setHgrow(Priority.ALWAYS);
                rowConstraints.setVgrow(Priority.NEVER);
                GridPane.setFillWidth(panelContent, true);
                break;
            default:
                LOGGER.warn("Unsupported position for panel step: {}", position);
                break;
        }

        overlayPane.getRowConstraints().add(rowConstraints);
        overlayPane.getColumnConstraints().add(columnConstraints);
        overlayPane.setVisible(true);

        if (step.resolver().isPresent()) {
            Optional<Node> targetNodeOpt = step.resolver().get().apply(parentStage.getScene());
            if (targetNodeOpt.isPresent()) {
                Node targetNode = targetNodeOpt.get();
                pulseIndicator.attachToNode(targetNode);
            } else {
                LOGGER.warn("Could not resolve target node for step: {}", step.title());
            }
        }
    }

    /**
     * Detaches the overlay and cleans up resources.
     */
    public void detach() {
        pulseIndicator.stop();

        overlayPane.setVisible(false);
        overlayPane.getChildren().clear();

        Scene scene = parentStage.getScene();
        if (scene != null && originalRoot != null) {
            stackContainer.getChildren().remove(originalRoot);
            scene.setRoot(originalRoot);
            LOGGER.debug("Restored original scene root: {}", originalRoot.getClass().getName());
        }
    }

    private void show() {
        overlayPane.setVisible(true);
        overlayPane.toFront();
    }

    private void hide() {
        overlayPane.setVisible(false);
    }
}
