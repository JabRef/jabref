package org.jabref.gui.walkthrough.effects;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import org.jabref.gui.walkthrough.WalkthroughUtils;

import com.sun.javafx.scene.TreeShowingProperty;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.NonNull;

/// Base class for walkthrough effects [Spotlight], [FullScreenDarken], and [Ping].
public sealed abstract class BaseWindowEffect permits Spotlight, FullScreenDarken, Ping {
    private static final long LAYOUT_DEBOUNCE_MS = 100;

    protected final Pane pane;
    protected final List<Subscription> subscriptions = new ArrayList<>();
    private ChangeListener<Number> windowSizeListener;
    private final WalkthroughUtils.DebouncedRunnable debouncedUpdateLayout;

    /// Constructor for WalkthroughEffect. No scene graph modification is done here. The
    /// effect is not attached to the pane until [#attach(Node)] is called.
    ///
    /// @param pane The pane where the effect will be applied. Usually obtained from
    ///             [Window#getScene()] and [Scene#getRoot()]
    protected BaseWindowEffect(@NonNull Pane pane) {
        this.pane = pane;
        this.debouncedUpdateLayout = WalkthroughUtils.debounced(this::updateLayout, LAYOUT_DEBOUNCE_MS);
    }

    /// The effect is no longer usable and permanently removed from the scene graph. To
    /// use this effect again, you can ONLY create a new instance of
    /// [BaseWindowEffect].
    public void detach() {
        hideEffect();
        cleanupListeners();
        cleanupWindowListeners();
    }

    /// Updates the layout of the effect. This method is called whenever the pane or the
    /// target node (if any) changes its size or position.
    protected abstract void updateLayout();

    /// Hides the effect. The WalkthroughEffect can still be shown again after this
    /// method is called by calling [BaseWindowEffect#updateLayout()], either manually
    /// or due to a layout change in the pane or the target node.
    protected abstract void hideEffect();

    protected void cleanupListeners() {
        debouncedUpdateLayout.cancel();
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }

    protected void setupListeners(@NonNull Node node) {
        subscriptions.add(EasyBind.subscribe(node.localToSceneTransformProperty(), _ -> debouncedUpdateLayout.run()));
        subscriptions.add(EasyBind.subscribe(node.boundsInLocalProperty(), _ -> debouncedUpdateLayout.run()));
        subscriptions.add(EasyBind.subscribe(new TreeShowingProperty(node), _ -> debouncedUpdateLayout.run()));

        Scene scene = node.getScene();
        if (scene != null) {
            subscriptions.add(EasyBind.subscribe(scene.widthProperty(), _ -> debouncedUpdateLayout.run()));
            subscriptions.add(EasyBind.subscribe(scene.heightProperty(), _ -> debouncedUpdateLayout.run()));

            setupWindowListeners(scene.getWindow());
        }

        node.sceneProperty().addListener((_, oldScene, newScene) -> {
            if (oldScene != null && oldScene.getWindow() != null) {
                cleanupWindowListeners();
            }
            if (newScene != null) {
                subscriptions.add(EasyBind.subscribe(newScene.widthProperty(), _ -> debouncedUpdateLayout.run()));
                subscriptions.add(EasyBind.subscribe(newScene.heightProperty(), _ -> debouncedUpdateLayout.run()));
                setupWindowListeners(newScene.getWindow());
            }
        });
    }

    protected void setupPaneListeners() {
        subscriptions.add(EasyBind.subscribe(pane.widthProperty(), _ -> debouncedUpdateLayout.run()));
        subscriptions.add(EasyBind.subscribe(pane.heightProperty(), _ -> debouncedUpdateLayout.run()));
        subscriptions.add(EasyBind.subscribe(pane.layoutBoundsProperty(), _ -> debouncedUpdateLayout.run()));
    }

    private void setupWindowListeners(Window window) {
        if (window != null) {
            windowSizeListener = (_, _, _) -> debouncedUpdateLayout.run();
            window.widthProperty().addListener(windowSizeListener);
            window.heightProperty().addListener(windowSizeListener);
            window.xProperty().addListener(windowSizeListener);
            window.yProperty().addListener(windowSizeListener);
        }
    }

    private void cleanupWindowListeners() {
        if (windowSizeListener != null && pane.getScene() != null && pane.getScene().getWindow() != null) {
            Window window = pane.getScene().getWindow();
            window.widthProperty().removeListener(windowSizeListener);
            window.heightProperty().removeListener(windowSizeListener);
            window.xProperty().removeListener(windowSizeListener);
            window.yProperty().removeListener(windowSizeListener);
            windowSizeListener = null;
        }
    }
}
