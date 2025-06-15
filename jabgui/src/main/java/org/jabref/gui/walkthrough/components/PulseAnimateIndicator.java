package org.jabref.gui.walkthrough.components;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import org.jspecify.annotations.NonNull;

/**
 * A pulsing circular indicator that can be attached to a target node.
 */
public class PulseAnimateIndicator extends WalkthroughEffect {
    private Circle pulseIndicator;
    private Timeline pulseAnimation;
    private Node node;

    public PulseAnimateIndicator(@NonNull Pane pane) {
        super(pane);
    }

    /**
     * Attaches the pulse indicator to the specified node.
     *
     * @param node The node to attach the pulse indicator to.
     */
    public void attach(@NonNull Node node) {
        cleanUp();
        if (pulseIndicator == null) {
            initializeEffect();
        }
        this.node = node;
        setupNodeListeners(this.node);
        updateLayout();
    }

    @Override
    public void detach() {
        pulseAnimation.stop();
        if (pulseIndicator.getParent() instanceof Pane parentPane) {
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

        pane.getChildren().add(pulseIndicator);

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
        if (cannotPositionNode(node)) {
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

        double indicatorX = targetBounds.getMaxX() - 5;
        double indicatorY = targetBounds.getMinY() + 5;

        pulseIndicator.setLayoutX(indicatorX);
        pulseIndicator.setLayoutY(indicatorY);
        pulseIndicator.toFront();
    }

    @Override
    protected void hideEffect() {
        pulseIndicator.setVisible(false);
    }
}
