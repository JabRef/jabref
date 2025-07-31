package org.jabref.gui.walkthrough;

import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A specialized StackPane that manages walkthrough overlays for a specific window.
/// Each window can have at most one WalkthroughPane instance.
///
/// @implNote This pane is created since [impl.org.controlsfx.skin.DecorationPane] from
/// ControlsFX also modifies the scene root, which can lead to issues with overlapping,
/// never-removed Walkthrough effects. To prevent this, we use a dedicated pane that can
/// be identified as part of the walkthrough system.
public class WalkthroughPane extends StackPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughPane.class);
    private static final ConcurrentHashMap<Window, WalkthroughPane> INSTANCES = new ConcurrentHashMap<>();

    private final Window window;
    private @Nullable Parent root;
    private volatile boolean isAttached = false;

    public WalkthroughPane(@NonNull Window window) {
        if (INSTANCES.containsKey(window)) {
            throw new IllegalStateException("WalkthroughPane already exists for this window: " + window.getClass().getSimpleName());
        }

        this.window = window;
        setMinSize(0, 0);
        INSTANCES.put(this.window, this);
    }

    /// Attaches this pane to the window by replacing the scene root. The original root
    /// becomes a child of this pane.
    public synchronized void attach() {
        if (isAttached) {
            LOGGER.debug("WalkthroughPane already attached to window: {}", window.getClass().getSimpleName());
            return;
        }

        Scene scene = window.getScene();
        if (scene == null) {
            LOGGER.warn("Cannot attach WalkthroughPane: window has no scene");
            return;
        }

        root = scene.getRoot();
        getChildren().add(root);
        scene.setRoot(this);
        isAttached = true;

        LOGGER.debug("WalkthroughPane attached to window: {}", window.getClass().getSimpleName());
    }

    /// Detaches this pane from the window by restoring the original scene root.
    public synchronized void detach() {
        INSTANCES.remove(window);

        if (!isAttached) {
            LOGGER.debug("WalkthroughPane not attached to window: {}", window.getClass().getSimpleName());
            return;
        }

        Scene scene = window.getScene();
        if (scene == null || root == null) {
            LOGGER.warn("Cannot detach WalkthroughPane: scene or original root is null");
            return;
        }

        getChildren().remove(root);
        scene.setRoot(root);
        root = null;
        isAttached = false;

        LOGGER.debug("WalkthroughPane detached from window: {}", window.getClass().getSimpleName());
    }

    public static @NonNull WalkthroughPane getInstance(@NonNull Window window) {
        return INSTANCES.computeIfAbsent(window, WalkthroughPane::new);
    }
}
