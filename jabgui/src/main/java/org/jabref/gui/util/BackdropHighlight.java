package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import com.sun.javafx.scene.NodeHelper;
import org.jspecify.annotations.NonNull;

/**
 * Creates a backdrop highlight effect.
 */
public class BackdropHighlight {
    private static final Color OVERLAY_COLOR = Color.rgb(0, 0, 0, 0.55);

    private final Pane pane;
    private Node targetNode;

    private final Rectangle backdrop;
    private final Rectangle hole;
    private @NonNull Shape overlayShape;

    private final List<Runnable> cleanUpTasks = new ArrayList<>();
    private final InvalidationListener updateListener = _ -> updateOverlayLayout();

    /**
     * Constructs a BackdropHighlight instance that overlays on the specified pane.
     *
     * @param pane The pane onto which the overlay will be added.
     */
    public BackdropHighlight(@NonNull Pane pane) {
        this.pane = pane;
        this.backdrop = new Rectangle();
        this.hole = new Rectangle();

        this.overlayShape = Shape.subtract(backdrop, hole);
        this.overlayShape.setFill(OVERLAY_COLOR);
        this.overlayShape.setVisible(false);

        this.pane.getChildren().add(overlayShape);
    }

    /**
     * Attaches the highlight effect to a specific node.
     *
     * @param node The node to be "highlighted".
     */
    public void attach(@NonNull Node node) {
        detach();

        this.targetNode = node;
        updateOverlayLayout();
        addListener(targetNode.localToSceneTransformProperty());
        addListener(targetNode.visibleProperty());
        addListener(pane.widthProperty());
        addListener(pane.heightProperty());
        addListener(pane.sceneProperty(), (_, _, newScene) -> {
            updateOverlayLayout();
            if (newScene == null) {
                return;
            }
            addListener(newScene.heightProperty());
            addListener(newScene.widthProperty());
            if (newScene.getWindow() == null) {
                return;
            }
            addListener(newScene.getWindow().widthProperty());
            addListener(newScene.getWindow().heightProperty());
        });
    }

    /**
     * Detaches the highlight effect from the target node. In other words, hide the
     * overlay.
     */
    public void detach() {
        cleanUpTasks.forEach(Runnable::run);
        cleanUpTasks.clear();
        overlayShape.setVisible(false);
        this.targetNode = null;
    }

    private <T> void addListener(ObservableValue<T> property) {
        property.addListener(updateListener);
        cleanUpTasks.add(() -> property.removeListener(updateListener));
    }

    private <T> void addListener(ObservableValue<T> property, ChangeListener<T> listener) {
        property.addListener(listener);
        cleanUpTasks.add(() -> property.removeListener(listener));
    }

    private void updateOverlayLayout() {
        if (targetNode == null || targetNode.getScene() == null) {
            overlayShape.setVisible(false);
            return;
        }

        // ref: https://stackoverflow.com/questions/43887427/alternative-for-removed-impl-istreevisible
        if (!NodeHelper.isTreeVisible(targetNode)) {
            overlayShape.setVisible(false);
            return;
        }

        Bounds nodeBoundsInScene;
        try {
            nodeBoundsInScene = targetNode.localToScene(targetNode.getBoundsInLocal());
        } catch (IllegalStateException e) {
            overlayShape.setVisible(false);
            return;
        }

        if (nodeBoundsInScene == null || nodeBoundsInScene.getWidth() <= 0 || nodeBoundsInScene.getHeight() <= 0) {
            overlayShape.setVisible(false);
            return;
        }

        backdrop.setX(0);
        backdrop.setY(0);
        backdrop.setWidth(pane.getWidth());
        backdrop.setHeight(pane.getHeight());

        Bounds nodeBoundsInRootPane = pane.sceneToLocal(nodeBoundsInScene);
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
}
