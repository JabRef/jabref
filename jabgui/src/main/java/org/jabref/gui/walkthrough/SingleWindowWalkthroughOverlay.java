package org.jabref.gui.walkthrough;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;

import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.TooltipPosition;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughNode;

import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the overlay for displaying walkthrough steps in a single window.
 */
public class SingleWindowWalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleWindowWalkthroughOverlay.class);

    private final Window window;
    private final GridPane overlayPane;
    private final Pane originalRoot;
    private final StackPane stackPane;
    private final WalkthroughRenderer renderer;
    private final List<Runnable> cleanUpTasks = new ArrayList<>();
    private PopOver currentPopOver;

    public SingleWindowWalkthroughOverlay(Window window) {
        this.window = window;
        this.renderer = new WalkthroughRenderer();

        overlayPane = new GridPane();
        overlayPane.getStyleClass().add("walkthrough-overlay");
        overlayPane.setPickOnBounds(false);
        overlayPane.setMaxWidth(Double.MAX_VALUE);
        overlayPane.setMaxHeight(Double.MAX_VALUE);

        Scene scene = window.getScene();
        assert scene != null;

        originalRoot = (Pane) scene.getRoot();
        stackPane = new StackPane();

        stackPane.getChildren().add(originalRoot);
        stackPane.getChildren().add(overlayPane);

        scene.setRoot(stackPane);
    }

    /**
     * Displays a walkthrough step with the specified target node.
     */
    public void displayStep(WalkthroughNode step, Node targetNode, Walkthrough walkthrough) {
        hide();
        displayStepContent(step, targetNode, walkthrough);
        overlayPane.toFront();
    }

    /**
     * Hide the overlay and clean up any resources.
     */
    public void hide() {
        if (currentPopOver != null) {
            currentPopOver.hide();
            currentPopOver = null;
        }

        overlayPane.getChildren().clear();
        overlayPane.setClip(null);
        overlayPane.setVisible(true);

        cleanUpTasks.forEach(Runnable::run);
        cleanUpTasks.clear();
    }

    /**
     * Detaches the overlay and restores the original scene root.
     */
    public void detach() {
        hide();

        Scene scene = window.getScene();
        if (scene != null && originalRoot != null) {
            stackPane.getChildren().remove(originalRoot);
            scene.setRoot(originalRoot);
            LOGGER.debug("Restored original scene root: {}", originalRoot.getClass().getName());
        }
    }

    private void displayStepContent(WalkthroughNode step, Node targetNode, Walkthrough walkthrough) {
        switch (step) {
            case TooltipStep tooltipStep -> {
                displayTooltipStep(tooltipStep, targetNode, walkthrough);
                hideOverlayPane();
            }
            case PanelStep panelStep -> {
                Node content = renderer.render(panelStep, walkthrough);
                displayPanelStep(content, panelStep);
                setupClipping(content);
            }
        }

        step.navigationPredicate().ifPresent(predicate ->
                cleanUpTasks.add(predicate.attachListeners(targetNode, walkthrough::nextStep)));
    }

    private void displayTooltipStep(TooltipStep step, Node targetNode, Walkthrough walkthrough) {
        Node content = renderer.render(step, walkthrough);

        currentPopOver = new PopOver();
        currentPopOver.setContentNode(content);
        currentPopOver.setDetachable(false);
        currentPopOver.setCloseButtonEnabled(false);
        currentPopOver.setHeaderAlwaysVisible(false);

        PopOver.ArrowLocation arrowLocation = mapToArrowLocation(step.position());
        if (arrowLocation != null) {
            currentPopOver.setArrowLocation(arrowLocation);
        }

        step.preferredWidth().ifPresent(width -> {
            currentPopOver.setPrefWidth(width);
            currentPopOver.setMinWidth(width);
        });
        step.preferredHeight().ifPresent(height -> {
            currentPopOver.setPrefHeight(height);
            currentPopOver.setMinHeight(height);
        });

        currentPopOver.show(targetNode);

        cleanUpTasks.add(() -> {
            if (currentPopOver != null) {
                currentPopOver.hide();
                currentPopOver = null;
            }
        });
    }

    private void displayPanelStep(Node content, PanelStep step) {
        overlayPane.getChildren().clear();
        overlayPane.getRowConstraints().clear();
        overlayPane.getColumnConstraints().clear();

        configurePanelLayout(step.position());

        overlayPane.getChildren().add(content);
        GridPane.setHgrow(content, Priority.NEVER);
        GridPane.setVgrow(content, Priority.NEVER);

        switch (step.position()) {
            case CENTER_LEFT -> {
                overlayPane.setAlignment(Pos.CENTER_LEFT);
                GridPane.setVgrow(content, Priority.ALWAYS);
                GridPane.setFillHeight(content, true);
            }
            case CENTER_RIGHT -> {
                overlayPane.setAlignment(Pos.CENTER_RIGHT);
                GridPane.setVgrow(content, Priority.ALWAYS);
                GridPane.setFillHeight(content, true);
            }
            case TOP_CENTER -> {
                overlayPane.setAlignment(Pos.TOP_CENTER);
                GridPane.setHgrow(content, Priority.ALWAYS);
                GridPane.setFillWidth(content, true);
            }
            case BOTTOM_CENTER -> {
                overlayPane.setAlignment(Pos.BOTTOM_CENTER);
                GridPane.setHgrow(content, Priority.ALWAYS);
                GridPane.setFillWidth(content, true);
            }
            default -> {
                LOGGER.warn("Unsupported position for panel step: {}", step.position());
                overlayPane.setAlignment(Pos.CENTER);
            }
        }
    }

    private void configurePanelLayout(Pos position) {
        RowConstraints rowConstraints = new RowConstraints();
        ColumnConstraints columnConstraints = new ColumnConstraints();

        switch (position) {
            case CENTER_LEFT,
                 CENTER_RIGHT -> {
                rowConstraints.setVgrow(Priority.ALWAYS);
                columnConstraints.setHgrow(Priority.NEVER);
            }
            case TOP_CENTER,
                 BOTTOM_CENTER -> {
                columnConstraints.setHgrow(Priority.ALWAYS);
                rowConstraints.setVgrow(Priority.NEVER);
            }
            default -> {
                rowConstraints.setVgrow(Priority.NEVER);
                columnConstraints.setHgrow(Priority.NEVER);
            }
        }

        overlayPane.getRowConstraints().add(rowConstraints);
        overlayPane.getColumnConstraints().add(columnConstraints);
    }

    private PopOver.ArrowLocation mapToArrowLocation(TooltipPosition position) {
        return switch (position) {
            case TOP ->
                    PopOver.ArrowLocation.TOP_CENTER;
            case BOTTOM ->
                    PopOver.ArrowLocation.BOTTOM_CENTER;
            case LEFT ->
                    PopOver.ArrowLocation.LEFT_CENTER;
            case RIGHT ->
                    PopOver.ArrowLocation.RIGHT_CENTER;
            case AUTO ->
                    null;
        };
    }

    private void hideOverlayPane() {
        overlayPane.setVisible(false);
        cleanUpTasks.add(() -> overlayPane.setVisible(true));
    }

    private void setupClipping(Node node) {
        ChangeListener<Bounds> listener = (_, _, bounds) -> {
            if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
                Rectangle clip = new Rectangle(bounds.getMinX(), bounds.getMinY(),
                        bounds.getWidth(), bounds.getHeight());
                overlayPane.setClip(clip);
            }
        };

        node.boundsInParentProperty().addListener(listener);

        Bounds initialBounds = node.getBoundsInParent();
        if (initialBounds.getWidth() > 0 && initialBounds.getHeight() > 0) {
            Rectangle clip = new Rectangle(initialBounds.getMinX(), initialBounds.getMinY(),
                    initialBounds.getWidth(), initialBounds.getHeight());
            overlayPane.setClip(clip);
        }

        cleanUpTasks.add(() -> node.boundsInParentProperty().removeListener(listener));
        cleanUpTasks.add(() -> overlayPane.setClip(null));
    }
}
