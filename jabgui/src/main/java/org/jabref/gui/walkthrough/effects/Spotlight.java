package org.jabref.gui.walkthrough.effects;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import org.jabref.gui.walkthrough.utils.WalkthroughUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class Spotlight extends BaseWindowEffect {
    private static final Duration TRANSITION_DURATION = Duration.millis(300);

    private @Nullable Node node;
    private Rectangle backdrop;
    private Rectangle hole;
    private volatile @Nullable Shape overlayShape;
    private @Nullable Runnable onClickHandler;
    private @Nullable Timeline transitionAnimation;

    /// Ensure the overlay shape is not updated concurrently. Consider a scenario where
    /// overlay shape is updated concurrently from two different threads,
    /// 1. We don't keep track of a new overlay shape is needed. Caller 1 first removed
    /// the overlay shape, thinking it needs to be removed. Caller 2 then realized no
    /// shape is present and added a new one.
    /// 2. Caller 1 continues (after context switch to caller 2) to finish updating the
    /// overlay shape without knowing that a new overlay shape has been added by caller
    /// 2.
    /// 3. Now, we have two overlay shapes in the pane, one from caller 1 and one from
    /// caller 2. We only have reference to the one from caller 2, but the one from
    /// caller 1 is still in the pane, leading to unexpected behavior.
    private final AtomicBoolean isUpdatingOverlayShape = new AtomicBoolean(false);

    public Spotlight(@NonNull Pane pane) {
        super(pane);
    }

    public void attach(@NonNull Node node) {
        if (this.node != null) {
            throw new IllegalStateException("Spotlight is already attached to a node. Detach it first.");
        }

        backdrop = new Rectangle();
        hole = new Rectangle();

        this.node = node;
        setupPaneListeners();
        setupListeners(node);
        updateLayout();
    }

    public void transitionTo(@NonNull Node newNode) {
        Shape overlayShape = this.overlayShape;
        if (overlayShape == null || !overlayShape.isVisible()) {
            attach(newNode);
            return;
        }

        if (transitionAnimation != null) {
            transitionAnimation.stop();
        }

        Bounds oldBounds = hole.getBoundsInParent();
        Bounds newBoundsInScene = newNode.localToScene(newNode.getBoundsInLocal());
        Bounds newBoundsInPane = pane.sceneToLocal(newBoundsInScene);

        hole.setX(oldBounds.getMinX());
        hole.setY(oldBounds.getMinY());
        hole.setWidth(oldBounds.getWidth());
        hole.setHeight(oldBounds.getHeight());

        transitionAnimation = new Timeline(
                new KeyFrame(TRANSITION_DURATION,
                        new KeyValue(hole.xProperty(), newBoundsInPane.getMinX(), Interpolator.EASE_BOTH),
                        new KeyValue(hole.yProperty(), newBoundsInPane.getMinY(), Interpolator.EASE_BOTH),
                        new KeyValue(hole.widthProperty(), newBoundsInPane.getWidth(), Interpolator.EASE_BOTH),
                        new KeyValue(hole.heightProperty(), newBoundsInPane.getHeight(), Interpolator.EASE_BOTH)
                )
        );

        ChangeListener<Number> changeListener = (_, _, _) -> updateOverlayShape();
        hole.xProperty().addListener(changeListener);
        hole.yProperty().addListener(changeListener);
        hole.widthProperty().addListener(changeListener);
        hole.heightProperty().addListener(changeListener);

        transitionAnimation.setOnFinished(_ -> {
            if (this.node != null) {
                cleanupListeners();
            }
            this.node = newNode;
            setupListeners(this.node);
            updateLayout();

            hole.xProperty().removeListener(changeListener);
            hole.yProperty().removeListener(changeListener);
            hole.widthProperty().removeListener(changeListener);
            hole.heightProperty().removeListener(changeListener);
        });

        transitionAnimation.play();
    }

    public void setOnClick(@Nullable Runnable onClickHandler) {
        this.onClickHandler = onClickHandler;
    }

    @Override
    public void detach() {
        if (node == null) {
            throw new IllegalStateException("Spotlight is not attached to any node.");
        }
        if (transitionAnimation != null) {
            transitionAnimation.stop();
            transitionAnimation = null;
        }
        super.detach();
        Shape overlayShape = this.overlayShape;
        if (overlayShape != null && overlayShape.getParent() instanceof Pane parentPane) {
            parentPane.getChildren().remove(overlayShape);
            this.overlayShape = null;
        }
        this.node = null;
    }

    @Override
    protected void updateLayout() {
        if (WalkthroughUtils.cannotPositionNode(node)) {
            hideEffect();
            return;
        }

        Bounds bounds = node.localToScene(node.getBoundsInLocal());

        if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            hideEffect();
            return;
        }

        backdrop.setX(0);
        backdrop.setY(0);
        backdrop.setWidth(pane.getWidth());
        backdrop.setHeight(pane.getHeight());

        Bounds nodeBoundsInRootPane = pane.sceneToLocal(bounds);
        hole.setX(nodeBoundsInRootPane.getMinX());
        hole.setY(nodeBoundsInRootPane.getMinY());
        hole.setWidth(nodeBoundsInRootPane.getWidth());
        hole.setHeight(nodeBoundsInRootPane.getHeight());

        updateOverlayShape();
    }

    @Override
    protected void hideEffect() {
        Shape overlayShape = this.overlayShape;
        if (overlayShape != null) {
            overlayShape.setVisible(false);
        }
    }

    private void updateOverlayShape() {
        if (isUpdatingOverlayShape.getAndSet(true)) {
            throw new IllegalStateException("Overlay shape is enjoying an update!");
        }

        int oldIndex;
        if (overlayShape == null || !pane.getChildren().contains(overlayShape)) {
            oldIndex = pane.getChildren().size();
        } else {
            oldIndex = this.pane.getChildren().indexOf(overlayShape);
            this.pane.getChildren().remove(oldIndex);
        }

        Shape overlayShape = Shape.subtract(backdrop, hole);
        overlayShape.getStyleClass().add("walkthrough-spotlight");

        if (onClickHandler != null) {
            overlayShape.setOnMouseClicked(this::handleClick);
            overlayShape.setMouseTransparent(false);
        } else {
            overlayShape.setMouseTransparent(true);
        }

        this.overlayShape = overlayShape;
        this.pane.getChildren().add(oldIndex, this.overlayShape);

        isUpdatingOverlayShape.set(false);
    }

    private void handleClick(MouseEvent event) {
        if (onClickHandler != null) {
            onClickHandler.run();
        }
        event.consume();
    }
}
