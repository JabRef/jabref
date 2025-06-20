package org.jabref.gui.walkthrough;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import org.jabref.gui.walkthrough.declarative.step.PanelPosition;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.TooltipPosition;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;

import org.controlsfx.control.PopOver;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Platform;

/**
 * Manages the overlay for displaying walkthrough steps in a single window.
 */
public class SingleWindowWalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleWindowWalkthroughOverlay.class);

    private final Window window;
    private final GridPane overlayPane;
    private final PopOver popover;
    private final Pane originalRoot;
    private final StackPane stackPane;
    private final WalkthroughRenderer renderer;
    private final List<Runnable> cleanUpTasks = new ArrayList<>();
    private @Nullable Node node;

    public SingleWindowWalkthroughOverlay(Window window) {
        this.window = window;
        this.renderer = new WalkthroughRenderer();

        overlayPane = new GridPane();
        overlayPane.getStyleClass().add("walkthrough-overlay");
        overlayPane.setPickOnBounds(false);
        overlayPane.setMaxWidth(Double.MAX_VALUE);
        overlayPane.setMaxHeight(Double.MAX_VALUE);

        popover = new PopOver();

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
    public void displayStep(WalkthroughStep step, @Nullable Node targetNode, Runnable beforeNavigate, Walkthrough walkthrough) {
        hide();
        displayStepContent(step, targetNode, beforeNavigate, walkthrough);
        node = targetNode;
    }

    /**
     * Hide the overlay and clean up any resources.
     */
    public void hide() {
        popover.hide();

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

    private void displayStepContent(WalkthroughStep step, Node targetNode, Runnable beforeNavigate, Walkthrough walkthrough) {
        switch (step) {
            case TooltipStep tooltipStep -> {
                Node content = renderer.render(tooltipStep, walkthrough, beforeNavigate);
                displayTooltipStep(content, targetNode, tooltipStep);
                hideOverlayPane();
            }
            case PanelStep panelStep -> {
                Node content = renderer.render(panelStep, walkthrough, beforeNavigate);
                displayPanelStep(content, panelStep);
                setupClipping(content);
                overlayPane.toFront();
            }
        }

        step.navigationPredicate().ifPresent(predicate -> {
            if (targetNode == null) {
                return;
            }
            cleanUpTasks.add(predicate.attachListeners(targetNode, beforeNavigate, walkthrough::nextStep));
        });
    }

    private void displayTooltipStep(Node content, @Nullable Node targetNode, TooltipStep step) {
        popover.setContentNode(content);
        popover.setDetachable(false);
        popover.setCloseButtonEnabled(false);
        popover.setHeaderAlwaysVisible(false);
        popover.setAutoFix(true);
        popover.setAutoHide(false);
        mapToArrowLocation(step.position()).ifPresent(popover::setArrowLocation);

        step.preferredWidth().ifPresent(width -> {
            popover.setPrefWidth(width);
            popover.setMinWidth(width);
        });
        step.preferredHeight().ifPresent(height -> {
            popover.setPrefHeight(height);
            popover.setMinHeight(height);
        });

        // Defer showing the popover until the next pulse to ensure the
        // target node (or window) has been fully laid out. This prevents
        // situations where the pop-over fails to appear because the node
        // is not yet ready (for example directly after a scene change).
        Platform.runLater(() -> {
            if (targetNode != null) {
                popover.show(targetNode);
            } else {
                popover.show(window);
            }
        });

        ChangeListener<Boolean> listener = (_, _, focused) -> {
            if (focused && !popover.isShowing()) {
                LOGGER.debug("Window gained focus, ensuring tooltip is visible");
                if (node != null) {
                    popover.show(node);
                } else {
                    popover.show(window);
                }
            }
        };

        window.focusedProperty().addListener(listener);
        cleanUpTasks.add(() -> window.focusedProperty().removeListener(listener));
        popover.showingProperty().addListener(listener);
        cleanUpTasks.add(() -> popover.showingProperty().removeListener(listener));
        cleanUpTasks.add(popover::hide);
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
            case LEFT -> {
                overlayPane.setAlignment(Pos.CENTER_LEFT);
                GridPane.setVgrow(content, Priority.ALWAYS);
                GridPane.setFillHeight(content, true);
            }
            case RIGHT -> {
                overlayPane.setAlignment(Pos.CENTER_RIGHT);
                GridPane.setVgrow(content, Priority.ALWAYS);
                GridPane.setFillHeight(content, true);
            }
            case TOP -> {
                overlayPane.setAlignment(Pos.TOP_CENTER);
                GridPane.setHgrow(content, Priority.ALWAYS);
                GridPane.setFillWidth(content, true);
            }
            case BOTTOM -> {
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

    private void configurePanelLayout(PanelPosition position) {
        RowConstraints rowConstraints = new RowConstraints();
        ColumnConstraints columnConstraints = new ColumnConstraints();

        switch (position) {
            case LEFT,
                 RIGHT -> {
                rowConstraints.setVgrow(Priority.ALWAYS);
                columnConstraints.setHgrow(Priority.NEVER);
            }
            case TOP,
                 BOTTOM -> {
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

    private Optional<PopOver.ArrowLocation> mapToArrowLocation(TooltipPosition position) {
        return Optional.ofNullable(switch (position) {
            case TOP ->
                    PopOver.ArrowLocation.BOTTOM_CENTER;
            case BOTTOM ->
                    PopOver.ArrowLocation.TOP_CENTER;
            case LEFT ->
                    PopOver.ArrowLocation.RIGHT_CENTER;
            case RIGHT ->
                    PopOver.ArrowLocation.LEFT_CENTER;
            case AUTO ->
                    null;
        });
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
