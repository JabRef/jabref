package org.jabref.gui.walkthrough.components;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import com.sun.javafx.scene.NodeHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Base class for walkthrough effects with common listener management and positioning.
 */
public abstract class WalkthroughEffect {
    protected final Pane pane;
    protected final List<Runnable> cleanupTasks = new ArrayList<>();
    protected final InvalidationListener updateListener = _ -> updateLayout();

    protected WalkthroughEffect(@NonNull Pane pane) {
        this.pane = pane;
        initializeEffect();
    }

    protected abstract void initializeEffect();

    protected abstract void updateLayout();

    protected abstract void hideEffect();

    /**
     * Detaches the effect, cleaning up listeners and hiding the effect.
     */
    public void detach() {
        cleanUp();
        hideEffect();
    }

    protected void cleanUp() {
        cleanupTasks.forEach(Runnable::run);
        cleanupTasks.clear();
    }

    protected <T> void addListener(ObservableValue<T> property) {
        property.addListener(updateListener);
        cleanupTasks.add(() -> property.removeListener(updateListener));
    }

    protected <T> void addListener(ObservableValue<T> property, ChangeListener<T> listener) {
        property.addListener(listener);
        cleanupTasks.add(() -> property.removeListener(listener));
    }

    protected void setupNodeListeners(@NonNull Node node) {
        addListener(node.boundsInLocalProperty());
        addListener(node.localToSceneTransformProperty());
        addListener(node.visibleProperty());

        ChangeListener<Scene> sceneListener = (_, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.widthProperty().removeListener(updateListener);
                oldScene.heightProperty().removeListener(updateListener);
            }
            if (newScene != null) {
                addListener(newScene.widthProperty());
                addListener(newScene.heightProperty());
                if (newScene.getWindow() != null) {
                    Window window = newScene.getWindow();
                    addListener(window.widthProperty());
                    addListener(window.heightProperty());
                    addListener(window.showingProperty());
                }
            }
            updateLayout();
        };

        addListener(node.sceneProperty(), sceneListener);
        if (node.getScene() != null) {
            sceneListener.changed(null, null, node.getScene());
        }
    }

    protected void setupPaneListeners() {
        addListener(pane.widthProperty());
        addListener(pane.heightProperty());
        addListener(pane.sceneProperty(), (_, _, newScene) -> {
            updateLayout();
            if (newScene == null) {
                return;
            }
            addListener(newScene.heightProperty());
            addListener(newScene.widthProperty());
            if (newScene.getWindow() != null) {
                addListener(newScene.getWindow().widthProperty());
                addListener(newScene.getWindow().heightProperty());
            }
        });
    }

    protected boolean isNodeVisible(@Nullable Node node) {
        return node != null && NodeHelper.isTreeVisible(node);
    }

    protected boolean cannotPositionNode(@Nullable Node node) {
        return node == null ||
                node.getScene() == null ||
                !isNodeVisible(node) ||
                node.getBoundsInLocal().isEmpty();
    }
}
