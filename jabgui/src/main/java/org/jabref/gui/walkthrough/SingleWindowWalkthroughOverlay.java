package org.jabref.gui.walkthrough;

import java.util.Optional;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;

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
    private final BorderPane overlayPane;
    private final Pane originalRoot;
    private final StackPane stackPane;
    private final WalkthroughRenderer renderer;
    private final WalkthroughUpdater updater = new WalkthroughUpdater();

    public SingleWindowWalkthroughOverlay(Window window) {
        this.window = window;
        this.renderer = new WalkthroughRenderer();

        overlayPane = new BorderPane();
        overlayPane.getStyleClass().add("walkthrough-overlay");
        overlayPane.setPickOnBounds(false);
        overlayPane.setMaxWidth(Double.MAX_VALUE);
        overlayPane.setMaxHeight(Double.MAX_VALUE);

        overlayPane.prefWidthProperty().bind(window.widthProperty());
        overlayPane.prefHeightProperty().bind(window.heightProperty());
        overlayPane.minWidthProperty().bind(window.widthProperty());
        overlayPane.minHeightProperty().bind(window.heightProperty());

        Scene scene = window.getScene();
        // This basically never happens, so only a development time check is needed
        assert scene != null;

        originalRoot = (Pane) scene.getRoot();
        stackPane = new StackPane(originalRoot, overlayPane);
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
                setupClipping(content, panelStep);
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
        overlayPane.setTop(null);
        overlayPane.setBottom(null);
        overlayPane.setLeft(null);
        overlayPane.setRight(null);
        overlayPane.setCenter(null);

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
        switch (step.position()) {
            case LEFT -> overlayPane.setLeft(content);
            case RIGHT -> overlayPane.setRight(content);
            case TOP -> overlayPane.setTop(content);
            case BOTTOM -> overlayPane.setBottom(content);
            default -> {
                LOGGER.warn("Unsupported position for panel step: {}", step.position());
                overlayPane.setCenter(content);
            }
        }
    }

    private Optional<PopOver.ArrowLocation> mapToArrowLocation(TooltipPosition position) {
        return Optional.ofNullable(switch (position) {
            case TOP -> PopOver.ArrowLocation.BOTTOM_CENTER;
            case BOTTOM -> PopOver.ArrowLocation.TOP_CENTER;
            case LEFT -> PopOver.ArrowLocation.RIGHT_CENTER;
            case RIGHT -> PopOver.ArrowLocation.LEFT_CENTER;
            case AUTO -> null;
        });
    }

    private void hideOverlayPane() {
        overlayPane.setVisible(false);
        updater.addCleanupTask(() -> overlayPane.setVisible(true));
    }

    private void setupClipping(Node node, PanelStep step) {
        ChangeListener<Bounds> listener = (_, _, _) -> {
            Bounds windowBounds = window.getScene().getRoot().getBoundsInLocal();
            Bounds nodeBounds = node.getBoundsInParent();

            if (windowBounds.getWidth() <= 0 || windowBounds.getHeight() <= 0) {
                return;
            }

            Rectangle clip = switch (step.position()) {
                case LEFT ->
                        new Rectangle(0, 0, nodeBounds.getWidth(), windowBounds.getHeight());
                case RIGHT ->
                        new Rectangle(Math.max(0, windowBounds.getWidth() - nodeBounds.getWidth()), 0,
                                nodeBounds.getWidth(), windowBounds.getHeight());
                case TOP ->
                        new Rectangle(0, 0, windowBounds.getWidth(), nodeBounds.getHeight());
                case BOTTOM ->
                        new Rectangle(0, Math.max(0, windowBounds.getHeight() - nodeBounds.getHeight()),
                                windowBounds.getWidth(), nodeBounds.getHeight());
            };

            overlayPane.setClip(clip);
        };

        updater.listen(node.boundsInParentProperty(), listener);
        updater.listen(overlayPane.boundsInLocalProperty(), listener);

        listener.changed(null, null, node.getBoundsInParent());
    }
}
