package org.jabref.gui.walkthrough;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Window;
import javafx.util.Duration;

import com.sun.javafx.scene.NodeHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Managing listeners and automatic cleanup in walkthrough
 */
public class WalkthroughUpdater {
    private final List<Runnable> cleanupTasks = new ArrayList<>(); // has to be mutable
    private final Timeline updateTimeline = new Timeline();

    /**
     * Adds a cleanup task to be executed when cleanup() is called.
     */
    public void addCleanupTask(Runnable task) {
        cleanupTasks.add(task);
    }

    public void listen(@NonNull ObservableValue<?> property, @NonNull InvalidationListener listener) {
        property.addListener(listener);
        cleanupTasks.add(() -> property.removeListener(listener));
    }

    public <T> void listen(@NonNull ObservableValue<T> property, @NonNull ChangeListener<T> listener) {
        property.addListener(listener);
        cleanupTasks.add(() -> property.removeListener(listener));
    }

    /**
     * Handles scroll events by executing the provided handler and scheduling
     * follow-up updates using a timeline.
     */
    public void handleScrollEvent(@NonNull Runnable handler) {
        handler.run();
        // Schedule updates at 50ms intervals for up to 250ms to handle scroll overshoot
        // debounced to prevent excessive updates
        for (int i = 0; i <= Math.min(5, 5 - updateTimeline.getKeyFrames().size()); i++) {
            KeyFrame keyFrame = new KeyFrame(Duration.millis(i * 50), _ -> handler.run());
            updateTimeline.getKeyFrames().add(keyFrame);
        }
        updateTimeline.play();
    }

    /**
     * Sets up listeners for a node
     */
    public void setupNodeListeners(@NonNull Node node, @NonNull Runnable updateHandler) {
        InvalidationListener updateListener = _ -> updateHandler.run();
        listen(node.boundsInLocalProperty(), updateListener);
        listen(node.localToSceneTransformProperty(), updateListener);
        listen(node.boundsInParentProperty(), updateListener);
        listen(node.visibleProperty(), updateListener);
        listen(node.layoutBoundsProperty(), updateListener);
        listen(node.managedProperty(), updateListener);
        setupScrollContainerListeners(node, updateHandler);
        setupSceneListeners(node, updateHandler);
    }

    /**
     * Sets up listeners for scroll containers (ScrollPane, ListView)
     */
    public void setupScrollContainerListeners(@NonNull Node node, @NonNull Runnable updateHandler) {
        EventHandler<ScrollEvent> scrollHandler = _ -> handleScrollEvent(updateHandler);

        Stream.iterate(node, Objects::nonNull, Node::getParent)
              .filter(p -> p instanceof ScrollPane || p instanceof ListView)
              .findFirst()
              .ifPresent(parent -> {
                  if (parent instanceof ScrollPane scrollPane) {
                      ChangeListener<Number> scrollListener = (_, _, _) -> handleScrollEvent(updateHandler);
                      listen(scrollPane.vvalueProperty(), scrollListener);
                  } else if (parent instanceof ListView<?> listView) {
                      listView.addEventFilter(ScrollEvent.ANY, scrollHandler);
                      cleanupTasks.add(() -> listView.removeEventFilter(ScrollEvent.ANY, scrollHandler));
                      listen(listView.focusModelProperty(), _ -> updateHandler.run());
                  }
              });

        node.addEventFilter(ScrollEvent.ANY, scrollHandler);
        cleanupTasks.add(() -> node.removeEventFilter(ScrollEvent.ANY, scrollHandler));
    }

    /**
     * Sets up listeners for scene and window property changes
     */
    public void setupSceneListeners(@NonNull Node node, @NonNull Runnable updateHandler) {
        ChangeListener<Scene> sceneListener = (_, _, scene) -> {
            updateHandler.run();
            if (scene == null) {
                return;
            }
            listen(scene.widthProperty(), _ -> updateHandler.run());
            listen(scene.heightProperty(), _ -> updateHandler.run());

            if (scene.getWindow() != null) {
                setupWindowListeners(scene.getWindow(), updateHandler);
            }
        };

        listen(node.sceneProperty(), sceneListener);
        if (node.getScene() != null) {
            sceneListener.changed(null, null, node.getScene());
        }
    }

    /**
     * Sets up listeners for window property changes.
     */
    public void setupWindowListeners(@NonNull Window window, @NonNull Runnable updateHandler) {
        listen(window.widthProperty(), _ -> updateHandler.run());
        listen(window.heightProperty(), _ -> updateHandler.run());
        listen(window.showingProperty(), _ -> updateHandler.run());
    }

    /**
     * Check if a node is visible in the scene graph
     */
    public static boolean isNodeVisible(@Nullable Node node) {
        return node != null && NodeHelper.isTreeVisible(node);
    }

    /**
     * Utility method to check if a node cannot be positioned
     */
    public static boolean cannotPositionNode(@Nullable Node node) {
        return node == null || node.getScene() == null || !isNodeVisible(node) || node.getBoundsInLocal().isEmpty();
    }

    /**
     * Cleans up all registered listeners and stops any active timelines
     */
    public void cleanup() {
        updateTimeline.stop();
        cleanupTasks.forEach(Runnable::run);
        cleanupTasks.clear();
    }
}
