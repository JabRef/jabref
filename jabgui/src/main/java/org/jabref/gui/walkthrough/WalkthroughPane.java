package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;

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
    /// Mutable map of all the instances created. Not thread-safe because the entire
    /// life-cycle of this class is used in JavaFX-thread.
    private static final Map<Window, WalkthroughPane> INSTANCES = new HashMap<>();

    private final Window window;
    private @Nullable Parent root;
    private boolean isAttached = false;

    private WalkthroughPane(@NonNull Window window) {
        this.window = window;
        setMinSize(0, 0);
    }

    /// Returns the WalkthroughPane instance for the specified window, creating and
    /// attach it if necessary.
    public static @NonNull WalkthroughPane getInstance(@NonNull Window window) {
        return INSTANCES.computeIfAbsent(window, key -> {
            WalkthroughPane newPane = new WalkthroughPane(key);
            newPane.attach();
            return newPane;
        });
    }

    private void attach() {
        if (isAttached) {
            LOGGER.error("WalkthroughPane already attached to window: {}", window.getClass().getSimpleName());
            throw new IllegalStateException("WalkthroughPane already attached to window: " + window.getClass().getSimpleName());
        }
        Scene scene = window.getScene();
        if (scene == null) {
            throw new IllegalStateException("Cannot attach WalkthroughPane: scene is null for window: " + window.getClass().getSimpleName());
        }

        root = scene.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot attach WalkthroughPane: original root is null for window: " + window.getClass().getSimpleName());
        }
        getChildren().add(root);
        scene.setRoot(this);
        LOGGER.debug("WalkthroughPane attached to window: {}", window.getClass().getSimpleName());
        isAttached = true;
    }

    /// Ensure the WalkthroughPane is detached from the window.
    public void detach() {
        if (!isAttached) {
            LOGGER.error("WalkthroughPane not attached to window: {}", window.getClass().getSimpleName());
            throw new IllegalStateException("WalkthroughPane not attached to window: " + window.getClass().getSimpleName());
        }
        Scene scene = window.getScene();
        if (scene == null || root == null) {
            throw new IllegalStateException("Cannot detach WalkthroughPane: scene or root is null for window: " + window.getClass().getSimpleName());
        }

        getChildren().remove(root);
        scene.setRoot(root);
        root = null;
        INSTANCES.remove(window);
        LOGGER.debug("WalkthroughPane detached from window: {}", window.getClass().getSimpleName());
        isAttached = false;
    }
}
