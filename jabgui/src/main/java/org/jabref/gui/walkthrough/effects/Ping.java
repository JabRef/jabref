package org.jabref.gui.walkthrough.effects;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import org.jabref.gui.walkthrough.utils.WalkthroughUtils;

import org.jspecify.annotations.NonNull;

public final class Ping extends BaseWindowEffect {
    public static final int INDICATOR_OFFSET = 4;
    private static final Duration TRANSITION_DURATION = Duration.millis(300);

    private Circle ping;
    private Timeline pingAnimation;
    private Timeline transitionAnimation;
    private Node node;

    public Ping(@NonNull Pane pane) {
        super(pane);
    }

    public void attach(@NonNull Node node) {
        if (this.node != null) {
            throw new IllegalStateException("Ping effect is already attached to a node. Detach it first.");
        }

        ping = new Circle(8);
        ping.setMouseTransparent(true);
        ping.setManaged(false);
        ping.getStyleClass().add("walkthrough-ping");
        pane.getChildren().add(ping);

        pingAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(ping.opacityProperty(), 1.0),
                        new KeyValue(ping.scaleXProperty(), 1.0),
                        new KeyValue(ping.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.seconds(0.5),
                        new KeyValue(ping.opacityProperty(), 0.6),
                        new KeyValue(ping.scaleXProperty(), 1.3),
                        new KeyValue(ping.scaleYProperty(), 1.3)),
                new KeyFrame(Duration.seconds(1.0),
                        new KeyValue(ping.opacityProperty(), 1.0),
                        new KeyValue(ping.scaleXProperty(), 1.0),
                        new KeyValue(ping.scaleYProperty(), 1.0))
        );

        pingAnimation.setCycleCount(Timeline.INDEFINITE);
        pingAnimation.play();

        this.node = node;
        setupListeners(this.node);
        setupPaneListeners();
        updateLayout();
    }

    public void transitionTo(@NonNull Node newNode) {
        if (ping == null || !ping.isVisible()) {
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
                        new KeyValue(ping.layoutXProperty(), targetX, Interpolator.EASE_BOTH),
                        new KeyValue(ping.layoutYProperty(), targetY, Interpolator.EASE_BOTH)
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
        if (pingAnimation != null) {
            pingAnimation.stop();
        }
        if (transitionAnimation != null) {
            transitionAnimation.stop();
        }
        if (ping != null && ping.getParent() instanceof Pane parentPane) {
            parentPane.getChildren().remove(ping);
        }
        super.detach();
        node = null;
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

        ping.setVisible(true);

        double indicatorX = targetBounds.getMaxX() - INDICATOR_OFFSET;
        double indicatorY = targetBounds.getMinY() + INDICATOR_OFFSET;

        ping.setLayoutX(indicatorX);
        ping.setLayoutY(indicatorY);
        ping.toFront();
    }

    @Override
    protected void hideEffect() {
        if (ping != null) {
            ping.setVisible(false);
        }
    }
}
