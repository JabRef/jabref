package org.jabref.gui.walkthrough.effects;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import org.jabref.gui.walkthrough.WalkthroughUtils;

import org.jspecify.annotations.NonNull;

public final class PulseAnimateIndicator extends BaseWindowEffect {
    public static final int INDICATOR_OFFSET = 5;

    private static final Duration TRANSITION_DURATION = Duration.millis(300);

    private Circle pulseIndicator;
    private Timeline pulseAnimation;
    private Timeline transitionAnimation;
    private Node node;

    public PulseAnimateIndicator(@NonNull Pane pane) {
        super(pane);
    }

    public void attach(@NonNull Node node) {
        if (pulseIndicator == null) {
            initializeEffect();
        }

        if (this.node != null) {
            cleanupListeners();
        }

        this.node = node;
        setupListeners(this.node);
        updateLayout();
    }

    public void transitionTo(@NonNull Node newNode) {
        if (pulseIndicator == null || !pulseIndicator.isVisible()) {
            attach(newNode);
            return;
        }

        if (transitionAnimation != null) {
            transitionAnimation.stop();
        }

        Bounds newBounds = newNode.localToScene(newNode.getBoundsInLocal());
        Bounds newBoundsInPane = pane.sceneToLocal(newBounds);

        double targetX = newBoundsInPane.getMaxX() - INDICATOR_OFFSET;
        double targetY = newBoundsInPane.getMinY() + INDICATOR_OFFSET;

        transitionAnimation = new Timeline(
                new KeyFrame(TRANSITION_DURATION,
                        new KeyValue(pulseIndicator.layoutXProperty(), targetX, Interpolator.EASE_BOTH),
                        new KeyValue(pulseIndicator.layoutYProperty(), targetY, Interpolator.EASE_BOTH)
                )
        );

        transitionAnimation.setOnFinished(_ -> {
            if (this.node != null) {
                cleanupListeners();
            }
            this.node = newNode;
            setupListeners(this.node);
        });

        transitionAnimation.play();
    }

    @Override
    public void detach() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
        }
        if (transitionAnimation != null) {
            transitionAnimation.stop();
        }
        if (pulseIndicator != null && pulseIndicator.getParent() instanceof Pane parentPane) {
            parentPane.getChildren().remove(pulseIndicator);
        }
        super.detach();
        node = null;
    }

    @Override
    protected void initializeEffect() {
        pulseIndicator = new Circle(8, Color.web("#50618F"));
        pulseIndicator.setMouseTransparent(true);
        pulseIndicator.setManaged(false);

        getOrAddToPane();

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

    @Override
    protected void updateLayout() {
        if (WalkthroughUtils.cannotPositionNode(node)) {
            hideEffect();
            return;
        }

        Bounds localBounds = node.getBoundsInLocal();
        Bounds targetBounds = pane.sceneToLocal(node.localToScene(localBounds));
        if (targetBounds == null) {
            hideEffect();
            return;
        }

        pulseIndicator.setVisible(true);

        double indicatorX = targetBounds.getMaxX() - INDICATOR_OFFSET;
        double indicatorY = targetBounds.getMinY() + INDICATOR_OFFSET;

        pulseIndicator.setLayoutX(indicatorX);
        pulseIndicator.setLayoutY(indicatorY);
        pulseIndicator.toFront();
    }

    @Override
    protected void hideEffect() {
        if (pulseIndicator != null) {
            pulseIndicator.setVisible(false);
        }
    }

    private void getOrAddToPane() {
        if (pulseIndicator != null && !pane.getChildren().contains(pulseIndicator)) {
            pane.getChildren().add(pulseIndicator);
        }
    }
}
