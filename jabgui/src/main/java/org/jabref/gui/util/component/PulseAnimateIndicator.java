package org.jabref.gui.util.component;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * A pulsing circular indicator that can be attached to a target node.
 */
public class PulseAnimateIndicator {
    private Circle pulseIndicator;
    private Timeline pulseAnimation;
    private final Pane rootPane;
    private Node attachedNode;

    private final List<Runnable> cleanupTasks = new ArrayList<>();
    private final ChangeListener<Object> updatePositionListener = (_, _, _) -> updatePosition();

    public PulseAnimateIndicator(Pane rootPane) {
        this.rootPane = rootPane;
    }

    public void attachToNode(Node targetNode) {
        stop();

        this.attachedNode = targetNode;
        setupIndicator();
        setupListeners();
        updatePosition();
        startAnimation();
    }

    private void setupIndicator() {
        pulseIndicator = new Circle(8, Color.web("#50618F"));
        pulseIndicator.setMouseTransparent(true);
        pulseIndicator.setManaged(false);

        if (!rootPane.getChildren().contains(pulseIndicator)) {
            rootPane.getChildren().add(pulseIndicator);
        }
        pulseIndicator.toFront();
    }

    private void setupListeners() {
        attachedNode.boundsInLocalProperty().addListener(updatePositionListener);
        cleanupTasks.add(() -> attachedNode.boundsInLocalProperty().removeListener(updatePositionListener));

        attachedNode.localToSceneTransformProperty().addListener(updatePositionListener);
        cleanupTasks.add(() -> attachedNode.localToSceneTransformProperty().removeListener(updatePositionListener));

        ChangeListener<Scene> sceneListener = (_, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.widthProperty().removeListener(updatePositionListener);
                oldScene.heightProperty().removeListener(updatePositionListener);
            }
            if (newScene != null) {
                newScene.widthProperty().addListener(updatePositionListener);
                newScene.heightProperty().addListener(updatePositionListener);
                cleanupTasks.add(() -> {
                    newScene.widthProperty().removeListener(updatePositionListener);
                    newScene.heightProperty().removeListener(updatePositionListener);
                });
            }
            updatePosition();
        };

        attachedNode.sceneProperty().addListener(sceneListener);
        cleanupTasks.add(() -> attachedNode.sceneProperty().removeListener(sceneListener));

        if (attachedNode.getScene() != null) {
            sceneListener.changed(null, null, attachedNode.getScene());
        }
    }

    private void startAnimation() {
        pulseAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(pulseIndicator.opacityProperty(), 1.0),
                        new KeyValue(pulseIndicator.scaleXProperty(), 1.0),
                        new KeyValue(pulseIndicator.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.seconds(0.5),
                        new KeyValue(pulseIndicator.opacityProperty(), 0.6),
                        new KeyValue(pulseIndicator.scaleXProperty(), 1.3),
                        new KeyValue(pulseIndicator.scaleYProperty(), 1.3)),
                new KeyFrame(Duration.seconds(1.0),
                        new KeyValue(pulseIndicator.opacityProperty(), 1.0),
                        new KeyValue(pulseIndicator.scaleXProperty(), 1.0),
                        new KeyValue(pulseIndicator.scaleYProperty(), 1.0))
        );
        pulseAnimation.setCycleCount(Timeline.INDEFINITE);
        pulseAnimation.play();
    }

    private void updatePosition() {
        if (pulseIndicator == null || attachedNode == null || !isNodeVisible()) {
            setIndicatorVisible(false);
            return;
        }

        Bounds localBounds = attachedNode.getBoundsInLocal();
        if (localBounds.isEmpty()) {
            setIndicatorVisible(false);
            return;
        }

        Bounds targetBoundsInScene = attachedNode.localToScene(localBounds);
        if (targetBoundsInScene == null || rootPane.getScene() == null) {
            setIndicatorVisible(false);
            return;
        }

        Bounds targetBoundsInRoot = rootPane.sceneToLocal(targetBoundsInScene);
        if (targetBoundsInRoot == null) {
            setIndicatorVisible(false);
            return;
        }

        setIndicatorVisible(true);
        positionIndicator(targetBoundsInRoot);
    }

    // FIXME: This check is still fail for some cases
    private boolean isNodeVisible() {
        return attachedNode.isVisible() &&
                attachedNode.getScene() != null &&
                attachedNode.getLayoutBounds().getWidth() > 0 &&
                attachedNode.getLayoutBounds().getHeight() > 0;
    }

    private void setIndicatorVisible(boolean visible) {
        if (pulseIndicator != null) {
            pulseIndicator.setVisible(visible);
        }
    }

    private void positionIndicator(Bounds targetBounds) {
        double indicatorX = targetBounds.getMaxX() - 5;
        double indicatorY = targetBounds.getMinY() + 5;

        pulseIndicator.setLayoutX(indicatorX);
        pulseIndicator.setLayoutY(indicatorY);
        pulseIndicator.toFront();
    }

    public void stop() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            pulseAnimation = null;
        }

        if (pulseIndicator != null && pulseIndicator.getParent() instanceof Pane parentPane) {
            parentPane.getChildren().remove(pulseIndicator);
            pulseIndicator = null;
        }

        cleanupTasks.forEach(Runnable::run);
        cleanupTasks.clear();

        attachedNode = null;
    }
}
