package org.jabref.gui.walkthrough.effects;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import org.jabref.gui.walkthrough.WalkthroughUpdater;

import org.jspecify.annotations.NonNull;

public class PulseAnimateIndicator extends WalkthroughEffect {
    public static final int INDICATOR_OFFSET = 5;
    private Circle pulseIndicator;
    private Timeline pulseAnimation;
    private Node node;

    public PulseAnimateIndicator(@NonNull Pane pane) {
        super(pane);
    }

    public void attach(@NonNull Node node) {
        updater.cleanup();
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
        if (WalkthroughUpdater.cannotPositionNode(node)) {
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
        pulseIndicator.setVisible(false);
    }
}
