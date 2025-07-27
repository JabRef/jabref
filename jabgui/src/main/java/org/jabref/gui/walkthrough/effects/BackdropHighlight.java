package org.jabref.gui.walkthrough.effects;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import org.jabref.gui.walkthrough.WalkthroughUtils;

import com.tobiasdiez.easybind.EasyBind;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class BackdropHighlight extends BaseWindowEffect {
    private static final Color OVERLAY_COLOR = Color.rgb(0, 0, 0, 0.55);
    private static final Duration TRANSITION_DURATION = Duration.millis(300);

    private @Nullable Node node;
    private Rectangle backdrop;
    private Rectangle hole;
    private Rectangle animatedHole;
    private @Nullable Shape overlayShape;
    private @Nullable Runnable onClickHandler;
    private @Nullable Timeline transitionAnimation;

    public BackdropHighlight(@NonNull Pane pane) {
        super(pane);
    }

    public void attach(@NonNull Node node) {
        if (overlayShape == null) {
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

        animatedHole.setX(oldBounds.getMinX());
        animatedHole.setY(oldBounds.getMinY());
        animatedHole.setWidth(oldBounds.getWidth());
        animatedHole.setHeight(oldBounds.getHeight());

        transitionAnimation = new Timeline(
                new KeyFrame(TRANSITION_DURATION,
                        new KeyValue(animatedHole.xProperty(), newBoundsInPane.getMinX(), Interpolator.EASE_BOTH),
                        new KeyValue(animatedHole.yProperty(), newBoundsInPane.getMinY(), Interpolator.EASE_BOTH),
                        new KeyValue(animatedHole.widthProperty(), newBoundsInPane.getWidth(), Interpolator.EASE_BOTH),
                        new KeyValue(animatedHole.heightProperty(), newBoundsInPane.getHeight(), Interpolator.EASE_BOTH)
                )
        );

        transitionAnimation.setOnFinished(_ -> {
            if (this.node != null) {
                cleanupListeners();
            }
            this.node = newNode;
            setupListeners(this.node);
            updateLayout();
        });

        animatedHole.xProperty().addListener((_, _, _) -> updateOverlayShape());
        animatedHole.yProperty().addListener((_, _, _) -> updateOverlayShape());
        animatedHole.widthProperty().addListener((_, _, _) -> updateOverlayShape());
        animatedHole.heightProperty().addListener((_, _, _) -> updateOverlayShape());

        transitionAnimation.play();
    }

    public void setOnClick(@Nullable Runnable onClickHandler) {
        this.onClickHandler = onClickHandler;
    }

    @Override
    public void detach() {
        if (transitionAnimation != null) {
            transitionAnimation.stop();
            transitionAnimation = null;
        }
        super.detach();
        if (overlayShape != null && overlayShape.getParent() instanceof Pane parentPane) {
            parentPane.getChildren().remove(overlayShape);
            overlayShape = null;
        }
        this.node = null;
    }

    @Override
    protected void initializeEffect() {
        this.backdrop = new Rectangle();
        this.hole = new Rectangle();
        this.animatedHole = new Rectangle();
        this.overlayShape = Shape.subtract(backdrop, hole);
        this.overlayShape.setFill(OVERLAY_COLOR);
        this.overlayShape.setVisible(false);

        getOrAddToPane();
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

        animatedHole.setX(hole.getX());
        animatedHole.setY(hole.getY());
        animatedHole.setWidth(hole.getWidth());
        animatedHole.setHeight(hole.getHeight());

        updateOverlayShape();
    }

    @Override
    protected void hideEffect() {
        if (overlayShape != null) {
            overlayShape.setVisible(false);
        }
    }

    @Override
    protected void setupListeners(@NonNull Node node) {
        super.setupListeners(node);
        subscriptions.add(EasyBind.subscribe(node.visibleProperty(), _ -> this.updateLayout()));
        subscriptions.add(EasyBind.subscribe(node.disabledProperty(), _ -> this.updateLayout()));
        subscriptions.add(EasyBind.subscribe(pane.widthProperty(), _ -> this.updateLayout()));
        subscriptions.add(EasyBind.subscribe(pane.heightProperty(), _ -> this.updateLayout()));
    }

    private void updateOverlayShape() {
        Shape oldOverlayShape = this.overlayShape;
        int oldIndex = getOrAddToPane();

        if (this.pane.getChildren().contains(oldOverlayShape)) {
            oldIndex = this.pane.getChildren().indexOf(oldOverlayShape);
            this.pane.getChildren().remove(oldIndex);
        }

        this.overlayShape = Shape.subtract(backdrop, animatedHole);
        this.overlayShape.setFill(OVERLAY_COLOR);
        this.overlayShape.setVisible(true);

        if (onClickHandler != null) {
            this.overlayShape.setOnMouseClicked(this::handleClick);
            this.overlayShape.setMouseTransparent(false);
        } else {
            this.overlayShape.setMouseTransparent(true);
        }

        this.pane.getChildren().add(oldIndex, this.overlayShape);
    }

    private int getOrAddToPane() {
        if (overlayShape != null && !pane.getChildren().contains(overlayShape)) {
            pane.getChildren().add(overlayShape);
            return pane.getChildren().size() - 1;
        }
        return -1;
    }

    private void handleClick(MouseEvent event) {
        if (onClickHandler != null) {
            onClickHandler.run();
        }
        event.consume();
    }
}
