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
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.TooltipPosition;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the overlay for displaying walkthrough steps in a single window.
 */
public class SingleWindowOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleWindowOverlay.class);
    private static final double MARGIN = 10.0;
    private static final double ARROW_OVERLAP = 3.0;
    private final Stage parentStage;
    private final GridPane overlayPane;
    private final Pane originalRoot;
    private final StackPane stackPane;
    private final WalkthroughRenderer renderer;
    private final List<Runnable> cleanUpTasks = new ArrayList<>();

    public SingleWindowOverlay(Stage stage) {
        this.parentStage = stage;
        this.renderer = new WalkthroughRenderer();

        overlayPane = new GridPane();
        overlayPane.setStyle("-fx-background-color: transparent;");
        overlayPane.setPickOnBounds(false);
        overlayPane.setMaxWidth(Double.MAX_VALUE);
        overlayPane.setMaxHeight(Double.MAX_VALUE);

        Scene scene = stage.getScene();
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
        overlayPane.getChildren().clear();
        cleanUpTasks.forEach(Runnable::run);
        cleanUpTasks.clear();

        displayStepContent(step, targetNode, walkthrough);
        overlayPane.toFront();
    }

    /**
     * Hide the overlay and clean up any resources.
     */
    public void hide() {
        overlayPane.getChildren().clear();
        cleanUpTasks.forEach(Runnable::run);
        cleanUpTasks.clear();
    }

    /**
     * Detaches the overlay and restores the original scene root.
     */
    public void detach() {
        hide();

        Scene scene = parentStage.getScene();
        if (scene != null && originalRoot != null) {
            stackPane.getChildren().remove(originalRoot);
            scene.setRoot(originalRoot);
            LOGGER.debug("Restored original scene root: {}", originalRoot.getClass().getName());
        }
    }

    private void displayStepContent(WalkthroughNode step, Node targetNode, Walkthrough walkthrough) {
        Node stepContent;
        if (step instanceof TooltipStep tooltipStep) {
            stepContent = renderer.render(tooltipStep, walkthrough);
            displayTooltipContent(stepContent, targetNode, tooltipStep);
        } else if (step instanceof PanelStep panelStep) {
            stepContent = renderer.render(panelStep, walkthrough);
            displayPanelContent(stepContent, panelStep.position());
        }

        step.navigationPredicate().ifPresent(predicate -> cleanUpTasks.add(predicate.attachListeners(targetNode, walkthrough::nextStep)));
    }

    private void displayTooltipContent(Node content, Node targetNode, TooltipStep step) {
        StackPane tooltipContainer = new StackPane();
        tooltipContainer.setStyle("-fx-background-color: transparent;");

        Polygon arrow = createArrow();
        arrow.getStyleClass().add("walkthrough-tooltip-arrow");
        arrow.setStyle("-fx-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);");

        content.setStyle(content.getStyle() + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);");

        tooltipContainer.getChildren().addAll(content, arrow);

        overlayPane.getChildren().clear();
        overlayPane.setAlignment(Pos.TOP_LEFT);
        overlayPane.getChildren().add(tooltipContainer);

        setupClipping(tooltipContainer);

        ChangeListener<Bounds> layoutListener = (_, _, newBounds) -> {
            if (newBounds.getWidth() > 0 && newBounds.getHeight() > 0) {
                positionTooltipWithArrow(tooltipContainer, content, arrow, targetNode, step);
            }
        };
        content.boundsInLocalProperty().addListener(layoutListener);

        if (content.getBoundsInLocal().getWidth() > 0) {
            positionTooltipWithArrow(tooltipContainer, content, arrow, targetNode, step);
        }

        cleanUpTasks.add(() -> content.boundsInLocalProperty().removeListener(layoutListener));
    }

    private void displayPanelContent(Node content, Pos position) {
        overlayPane.getChildren().clear();
        overlayPane.getChildren().add(content);

        setupClipping(content);

        overlayPane.getRowConstraints().clear();
        overlayPane.getColumnConstraints().clear();
        overlayPane.setAlignment(position);

        GridPane.setHgrow(content, Priority.NEVER);
        GridPane.setVgrow(content, Priority.NEVER);
        GridPane.setFillWidth(content, false);
        GridPane.setFillHeight(content, false);

        RowConstraints rowConstraints = new RowConstraints();
        ColumnConstraints columnConstraints = new ColumnConstraints();

        switch (position) {
            case CENTER_LEFT:
            case CENTER_RIGHT:
                rowConstraints.setVgrow(Priority.ALWAYS);
                columnConstraints.setHgrow(Priority.NEVER);
                GridPane.setFillHeight(content, true);
                break;
            case TOP_CENTER:
            case BOTTOM_CENTER:
                columnConstraints.setHgrow(Priority.ALWAYS);
                rowConstraints.setVgrow(Priority.NEVER);
                GridPane.setFillWidth(content, true);
                break;
            default:
                LOGGER.warn("Unsupported position for panel step: {}", position);
                break;
        }

        overlayPane.getRowConstraints().add(rowConstraints);
        overlayPane.getColumnConstraints().add(columnConstraints);
    }

    private void setupClipping(Node node) {
        ChangeListener<? super Bounds> listener = (_, _, bounds) -> {
            Rectangle clip = new Rectangle(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
            overlayPane.setClip(clip);
        };
        node.boundsInParentProperty().addListener(listener);
        cleanUpTasks.add(() -> node.boundsInParentProperty().removeListener(listener));
        cleanUpTasks.add(() -> overlayPane.setClip(null));
    }

    private Polygon createArrow() {
        Polygon arrow = new Polygon();
        arrow.getPoints().addAll(new Double[] {
                0.0, 0.0,
                12.0, 15.0,
                -12.0, 15.0
        });
        return arrow;
    }

    private void positionTooltipWithArrow(StackPane container, Node tooltip, Node arrow, Node target, TooltipStep step) {
        Scene scene = parentStage.getScene();
        if (scene == null) {
            return;
        }

        Bounds targetBounds = target.localToScene(target.getBoundsInLocal());
        if (targetBounds == null) {
            LOGGER.warn("Could not determine bounds for target node.");
            return;
        }

        Bounds tooltipBounds = tooltip.getBoundsInLocal();
        double tooltipWidth = tooltipBounds.getWidth();
        double tooltipHeight = tooltipBounds.getHeight();

        TooltipPosition finalPosition = determinePosition(step.position(), targetBounds, tooltipWidth, tooltipHeight, scene);

        double tooltipX = switch (finalPosition) {
            case TOP, BOTTOM ->
                    targetBounds.getMinX() + (targetBounds.getWidth() - tooltipWidth) / 2;
            case LEFT -> targetBounds.getMinX() - tooltipWidth - MARGIN;
            case RIGHT, AUTO -> targetBounds.getMaxX() + MARGIN;
        };

        double tooltipY = switch (finalPosition) {
            case TOP -> targetBounds.getMinY() - tooltipHeight - MARGIN;
            case BOTTOM -> targetBounds.getMaxY() + MARGIN;
            case LEFT, RIGHT, AUTO ->
                    targetBounds.getMinY() + (targetBounds.getHeight() - tooltipHeight) / 2;
        };

        if (finalPosition == TooltipPosition.TOP || finalPosition == TooltipPosition.BOTTOM) {
            tooltipX = clamp(tooltipX, MARGIN, scene.getWidth() - tooltipWidth - MARGIN);
        } else {
            tooltipY = clamp(tooltipY, MARGIN, scene.getHeight() - tooltipHeight - MARGIN);
        }

        container.setTranslateX(tooltipX);
        container.setTranslateY(tooltipY);

        double targetCenterX = targetBounds.getMinX() + targetBounds.getWidth() / 2;
        double targetCenterY = targetBounds.getMinY() + targetBounds.getHeight() / 2;

        positionArrow(arrow, finalPosition, tooltipWidth, tooltipHeight,
                targetCenterX - tooltipX, targetCenterY - tooltipY);
    }

    private TooltipPosition determinePosition(TooltipPosition position, Bounds targetBounds,
                                              double tooltipWidth, double tooltipHeight, Scene scene) {
        if (position != TooltipPosition.AUTO) {
            return position;
        }

        if (targetBounds.getMaxY() + tooltipHeight + MARGIN < scene.getHeight()) {
            return TooltipPosition.BOTTOM;
        } else if (targetBounds.getMinY() - tooltipHeight - MARGIN > 0) {
            return TooltipPosition.TOP;
        } else if (targetBounds.getMaxX() + tooltipWidth + MARGIN < scene.getWidth()) {
            return TooltipPosition.RIGHT;
        } else if (targetBounds.getMinX() - tooltipWidth - MARGIN > 0) {
            return TooltipPosition.LEFT;
        }
        return TooltipPosition.BOTTOM;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // FIXME: Tweak the padding/margin values to ensure the arrow is positioned correctly
    private void positionArrow(Node arrow, TooltipPosition position, double tooltipWidth,
                               double tooltipHeight, double targetRelX, double targetRelY) {
        double arrowX = 0;
        double arrowY = 0;
        double rotation = 0;

        switch (position) {
            case TOP -> {
                arrowX = clamp(targetRelX, 10, tooltipWidth - 30);
                arrowY = tooltipHeight - ARROW_OVERLAP;
                rotation = 180;
            }
            case BOTTOM -> {
                arrowX = clamp(targetRelX, 10, tooltipWidth - 30);
                arrowY = -15 + ARROW_OVERLAP;
                rotation = 0;
            }
            case LEFT -> {
                arrowX = tooltipWidth - ARROW_OVERLAP;
                arrowY = clamp(targetRelY, 10, tooltipHeight - 30);
                rotation = 90;
            }
            case RIGHT, AUTO -> {
                arrowX = -15 + ARROW_OVERLAP;
                arrowY = clamp(targetRelY, 10, tooltipHeight - 30);
                rotation = -90;
            }
        }

        arrow.setManaged(false);
        arrow.setLayoutX(arrowX);
        arrow.setLayoutY(arrowY);
        arrow.setRotate(rotation);
    }
}
