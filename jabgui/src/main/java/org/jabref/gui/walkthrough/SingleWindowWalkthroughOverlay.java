package org.jabref.gui.walkthrough;

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
    private final WalkthroughUpdater updater = new WalkthroughUpdater();

    public SingleWindowWalkthroughOverlay(Window window) {
        this.window = window;
        this.renderer = new WalkthroughRenderer();

        overlayPane = new GridPane();
        overlayPane.getStyleClass().add("walkthrough-overlay");
        overlayPane.setPickOnBounds(false);
        overlayPane.setMaxWidth(Double.MAX_VALUE);
        overlayPane.setMaxHeight(Double.MAX_VALUE);

        Scene scene = window.getScene();
        // This basically never happens, so only a development time check is needed
        assert scene != null;

        originalRoot = (Pane) scene.getRoot();
        stackPane = new StackPane();

        stackPane.getChildren().add(originalRoot);
        stackPane.getChildren().add(overlayPane);

        scene.setRoot(stackPane);
    }

    /// Display a walkthrough step without a target node.
    public void displayStep(WalkthroughStep step, Runnable beforeNavigate, Walkthrough walkthrough) {
        displayStep(step, null, beforeNavigate, walkthrough);
    }

    /// Displays a walkthrough step, with or without a target node.
    public void displayStep(WalkthroughStep step,
                            @Nullable Node targetNode,
                            Runnable beforeNavigate,
                            Walkthrough walkthrough) {
        hide();

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

        if (targetNode == null) {
            return;
        }

        step.navigationPredicate().ifPresent(predicate -> updater
                .addCleanupTask(predicate.attachListeners(targetNode, beforeNavigate, walkthrough::nextStep)));
    }

    /**
     * Hide the overlay and clean up any resources.
     */
    public void hide() {
        overlayPane.getChildren().clear();
        overlayPane.setClip(null);
        overlayPane.setVisible(true);
        updater.cleanup();
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

    private void displayTooltipStep(Node content, @Nullable Node targetNode, TooltipStep step) {
        PopOver popover = new PopOver();
        popover.getScene().getStylesheets().setAll(window.getScene().getStylesheets()); // FIXME: walkaround to prevent popover from not properly inheriting styles
        popover.setContentNode(content);
        popover.setDetachable(false);
        popover.setCloseButtonEnabled(false);
        popover.setHeaderAlwaysVisible(false);
        mapToArrowLocation(step.position()).ifPresent(popover::setArrowLocation);
        popover.setAutoHide(false);
        popover.setAutoFix(true);

        if (targetNode == null) {
            popover.show(window);
            return;
        }

        popover.show(targetNode);
        updater.addCleanupTask(popover::hide);
        Runnable showPopover = () -> {
            if (WalkthroughUpdater.cannotPositionNode(targetNode)) {
                return;
            }
            popover.show(targetNode);
        };
        updater.setupScrollContainerListeners(targetNode, showPopover);
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
        overlayPane.getRowConstraints().add(switch (position) {
            case LEFT,
                 RIGHT -> {
                RowConstraints rowConstraints = new RowConstraints();
                rowConstraints.setVgrow(Priority.ALWAYS);
                yield rowConstraints;
            }
            case TOP,
                 BOTTOM -> {
                RowConstraints rowConstraints = new RowConstraints();
                rowConstraints.setVgrow(Priority.NEVER);
                yield rowConstraints;
            }
        });
        overlayPane.getColumnConstraints().add(switch (position) {
            case LEFT,
                 RIGHT -> {
                ColumnConstraints columnConstraints = new ColumnConstraints();
                columnConstraints.setHgrow(Priority.NEVER);
                yield columnConstraints;
            }
            case TOP,
                 BOTTOM -> {
                ColumnConstraints columnConstraints = new ColumnConstraints();
                columnConstraints.setHgrow(Priority.ALWAYS);
                yield columnConstraints;
            }
        });
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
        updater.addCleanupTask(() -> overlayPane.setVisible(true));
    }

    private void setupClipping(Node node) {
        ChangeListener<Bounds> listener = (_, _, bounds) -> {
            if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
                Rectangle clip = new Rectangle(bounds.getMinX(), bounds.getMinY(),
                        bounds.getWidth(), bounds.getHeight());
                overlayPane.setClip(clip);
            }
        };
        updater.listen(node.boundsInLocalProperty(), listener);
        listener.changed(null, null, node.getBoundsInParent());
    }
}
