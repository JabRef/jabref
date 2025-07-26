package org.jabref.gui.walkthrough;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.PopOver;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the overlay for displaying walkthrough steps in a single window.
public class WindowOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowOverlay.class);

    private final Window window;
    private final GridPane overlayPane;
    private final Pane originalRoot;
    private final StackPane stackPane;
    private final WalkthroughRenderer renderer;
    private final Walkthrough walkthrough;
    private final List<Runnable> cleanupTasks = new ArrayList<>();

    public WindowOverlay(Window window, Walkthrough walkthrough) {
        this.window = window;
        this.renderer = new WalkthroughRenderer();
        this.walkthrough = walkthrough;

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

    /// Display a tooltip for the given step at the specified node.
    ///
    /// @param step           The step to display.
    /// @param node           The node to anchor the tooltip to, or null to show it at
    ///                       the window.
    /// @param beforeNavigate A runnable to execute before navigating to the next step.
    ///                       More precisely, the runnable to execute immediately upon
    ///                       the button press before Walkthrough's state change to the
    ///                       next step and before the original button/node's action is
    ///                       executed. Usually used to prevent automatic revert from
    ///                       unexpected reverting to the previous step when the node is
    ///                       not yet ready to be displayed
    /// @see WindowOverlay#showPanel(PanelStep, Runnable)
    /// @see WindowOverlay#showPanel(PanelStep, Node, Runnable)
    public void showTooltip(TooltipStep step, @Nullable Node node, Runnable beforeNavigate) {
        hide();

        Node content = renderer.render(step, walkthrough, beforeNavigate);
        PopOver popover = new PopOver();
        popover.getScene().getStylesheets().setAll(window.getScene().getStylesheets()); // FIXME: walkaround to prevent popover from not properly inheriting styles
        popover.setContentNode(content);
        popover.setDetachable(false);
        popover.setCloseButtonEnabled(false);
        popover.setHeaderAlwaysVisible(false);
        mapToArrowLocation(step.position()).ifPresent(popover::setArrowLocation);
        popover.setAutoHide(false);
        popover.setAutoFix(true);

        cleanupTasks.add(popover::hide);
        overlayPane.setVisible(false);

        if (node == null) {
            popover.show(window);
            return;
        }

        popover.show(node);

        cleanupTasks.add(EasyBind.subscribe(node.localToSceneTransformProperty(), _ -> {
            if (WalkthroughUtils.cannotPositionNode(node)) {
                return;
            }
            popover.show(node);
        })::unsubscribe);
        step.navigationPredicate().ifPresent(predicate ->
                cleanupTasks.add(predicate.attachListeners(node, beforeNavigate, walkthrough::nextStep)));
    }

    public void showPanel(PanelStep step, Runnable beforeNavigate) {
        showPanel(step, null, beforeNavigate);
    }

    /// Display a Panel for the given step at the specified node.
    ///
    /// @param step           The step to display.
    /// @param node           The node to anchor highlight to (e.g., BackdropHighlight
    ///                       may poke a hole at the position of the node), or null to
    ///                       use fallback effect of corresponding position.
    /// @param beforeNavigate A runnable to execute before navigating to the next step.
    ///                       More precisely, the runnable to execute immediately upon
    ///                       the button press before Walkthrough's state change to the
    ///                       next step and before the original button/node's action is
    ///                       executed. Usually used to prevent automatic revert from
    ///                       unexpected reverting to the previous step when the node is
    ///                       not yet ready to be displayed
    /// @see WindowOverlay#showPanel(PanelStep, Runnable)
    /// @see WindowOverlay#showTooltip(TooltipStep, Node, Runnable)
    public void showPanel(PanelStep step, @Nullable Node node, Runnable beforeNavigate) {
        hide();

        overlayPane.setVisible(true);

        Node content = renderer.render(step, walkthrough, beforeNavigate);
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
        setupClipping(content);
        overlayPane.toFront();

        if (node != null) {
            step.navigationPredicate().ifPresent(predicate ->
                    cleanupTasks.add(predicate.attachListeners(node, beforeNavigate, walkthrough::nextStep)));
        }
    }

    /// Hide the overlay and clean up any resources.
    public void hide() {
        overlayPane.getChildren().clear();
        overlayPane.setClip(null);
        overlayPane.setVisible(false);
        cleanupTasks.forEach(Runnable::run);
        cleanupTasks.clear();
    }

    /// Detaches the overlay and restores the original scene root.
    public void detach() {
        hide();

        Scene scene = window.getScene();
        if (scene != null && originalRoot != null) {
            stackPane.getChildren().remove(originalRoot);
            scene.setRoot(originalRoot);
            LOGGER.debug("Restored original scene root: {}", originalRoot.getClass().getName());
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
            case TOP -> PopOver.ArrowLocation.BOTTOM_CENTER;
            case BOTTOM -> PopOver.ArrowLocation.TOP_CENTER;
            case LEFT -> PopOver.ArrowLocation.RIGHT_CENTER;
            case RIGHT -> PopOver.ArrowLocation.LEFT_CENTER;
            case AUTO -> null;
        });
    }

    private void setupClipping(Node node) {
        ChangeListener<Bounds> listener = (_, _, bounds) -> {
            if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
                Rectangle clip = new Rectangle(bounds.getMinX(), bounds.getMinY(),
                        bounds.getWidth(), bounds.getHeight());
                overlayPane.setClip(clip);
            }
        };
        ObservableValue<Bounds> property = node.boundsInLocalProperty();
        property.addListener(listener);
        cleanupTasks.add(() -> property.removeListener(listener));
        listener.changed(null, null, node.getBoundsInParent());
    }
}
