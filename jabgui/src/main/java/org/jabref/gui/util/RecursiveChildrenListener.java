package org.jabref.gui.util;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class RecursiveChildrenListener {
    private final ListChangeListener<Node> childrenListener;
    private @Nullable ChangeListener<Parent> sceneRootListener;
    private @Nullable Scene attachedScene;
    private @Nullable Parent attachedRoot;

    /// Creates a new RecursiveChildrenListener that will recursively monitor changes in
    /// the children of all parent nodes in a scene graph.
    ///
    /// @param childrenListener the listener to be called when children change
    public RecursiveChildrenListener(@NonNull ListChangeListener<Node> childrenListener) {
        this.childrenListener = childrenListener;
    }

    /// Attaches the listener to a scene, monitoring its root and all descendants. If
    /// the scene's root changes, the listener will automatically detach from the old
    /// root and attach to the new one.
    ///
    /// @param scene the scene to attach to
    public void attachToScene(@NonNull Scene scene) {
        detach();
        this.attachedScene = scene;

        sceneRootListener = (_, oldRoot, newRoot) -> {
            if (oldRoot != null) {
                removeChildrenListener(oldRoot);
            }
            if (newRoot != null) {
                attachedRoot = newRoot;
                addChildrenListener(newRoot);
            }
        };
        scene.rootProperty().addListener(sceneRootListener);

        Parent currentRoot = scene.getRoot();
        if (currentRoot != null) {
            attachedRoot = currentRoot;
            addChildrenListener(currentRoot);
        }
    }

    /// Attaches the listener directly to a node and all its descendants. If the node is
    /// not a Parent, the listener will not be attached.
    ///
    /// @param node the node to attach to
    public void attachToNode(@NonNull Node node) {
        detach();
        if (node instanceof Parent parent) {
            attachedRoot = parent;
            addChildrenListener(parent);
        }
    }

    /// Detaches all listeners from the scene or node hierarchy.
    public void detach() {
        if (attachedScene != null && sceneRootListener != null) {
            attachedScene.rootProperty().removeListener(sceneRootListener);
            sceneRootListener = null;
            attachedScene = null;
        }

        if (attachedRoot != null) {
            removeChildrenListener(attachedRoot);
            attachedRoot = null;
        }
    }

    private void addChildrenListener(Parent parent) {
        parent.getChildrenUnmodifiable().addListener(childrenListener);
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Parent childParent) {
                addChildrenListener(childParent);
            }
        }
    }

    private void removeChildrenListener(Parent parent) {
        parent.getChildrenUnmodifiable().removeListener(childrenListener);
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Parent childParent) {
                removeChildrenListener(childParent);
            }
        }
    }
}
