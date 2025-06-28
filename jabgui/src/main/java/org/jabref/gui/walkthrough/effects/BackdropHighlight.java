package org.jabref.gui.walkthrough.effects;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import org.jabref.gui.walkthrough.WalkthroughUpdater;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class BackdropHighlight extends WalkthroughEffect {
    private static final Color OVERLAY_COLOR = Color.rgb(0, 0, 0, 0.55);

    private @Nullable Node node;
    private Rectangle backdrop;
    private Rectangle hole;
    private @Nullable Shape overlayShape;

    public BackdropHighlight(@NonNull Pane pane) {
        super(pane);
    }

    public void attach(@NonNull Node node) {
        detach();
        if (overlayShape == null) {
            initializeEffect();
        }
        this.node = node;
        setupNodeListeners(this.node);
        setupPaneListeners();
        updateLayout();
    }

    @Override
    public void detach() {
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
        this.overlayShape = Shape.subtract(backdrop, hole);
        this.overlayShape.setFill(OVERLAY_COLOR);
        this.overlayShape.setVisible(false);
        this.pane.getChildren().add(overlayShape);
    }

    @Override
    protected void updateLayout() {
        if (WalkthroughUpdater.cannotPositionNode(node)) {
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

        Shape oldOverlayShape = this.overlayShape;
        int oldIndex = -1;
        if (this.pane.getChildren().contains(oldOverlayShape)) {
            oldIndex = this.pane.getChildren().indexOf(oldOverlayShape);
            this.pane.getChildren().remove(oldIndex);
        }

        this.overlayShape = Shape.subtract(backdrop, hole);
        this.overlayShape.setFill(OVERLAY_COLOR);
        this.overlayShape.setVisible(true);
        this.pane.getChildren().add(oldIndex, this.overlayShape);
    }

    @Override
    protected void hideEffect() {
        assert overlayShape != null : "Overlay shape should be initialized before hiding effect";
        overlayShape.setVisible(false);
    }
}
