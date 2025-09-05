package org.jabref.gui.walkthrough.effects;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import org.jabref.gui.walkthrough.utils.WalkthroughUtils;

import com.tobiasdiez.easybind.EasyBind;
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

        InvalidationListener updater = _ -> updateOverlayShape();
        subscriptions.add(EasyBind.listen(hole.xProperty(), updater));
        subscriptions.add(EasyBind.listen(hole.yProperty(), updater));
        subscriptions.add(EasyBind.listen(hole.widthProperty(), updater));
        subscriptions.add(EasyBind.listen(hole.heightProperty(), updater));

        transitionAnimation.setOnFinished(_ -> {
            if (this.node != null) {
                cleanupListeners();
            }
            this.node = newNode;
            setupListeners(this.node);
            updateLayout();
        });

        transitionAnimation.play();
    }

    public void setOnClick(@Nullable Runnable onClickHandler) {
        this.onClickHandler = onClickHandler;
    }

    @Override
    public void detach() {
        super.detach();
        if (node == null) {
            throw new IllegalStateException("Spotlight is not attached to any node.");
        }
        if (transitionAnimation != null) {
            transitionAnimation.stop();
            transitionAnimation = null;
        }
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
            if (overlayShape == null) {
                // The callee must guarantee the node to be attached is at least visible
                // in the scene to begin with. If such exception is thrown, it indicates
                // a bug in the caller side. Consider adjusting WalkthroughResolver.
                throw new IllegalStateException("Failed to attach the node.");
            }
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
    }

    private void handleClick(MouseEvent event) {
        if (onClickHandler != null) {
            onClickHandler.run();
        }
        event.consume();
    }
}
