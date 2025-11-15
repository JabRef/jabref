package org.jabref.gui.walkthrough.effects;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import org.jabref.gui.walkthrough.utils.WalkthroughUtils;

import com.sun.javafx.scene.TreeShowingProperty;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.NonNull;

/// Base class for walkthrough effects [Spotlight], [FullScreenDarken], and [Ping].
public sealed abstract class BaseWindowEffect permits Spotlight, FullScreenDarken, Ping {
    protected final Pane pane;
    protected final List<Subscription> subscriptions = new ArrayList<>();
    private final WalkthroughUtils.DebouncedInvalidationListener debouncedUpdater;

    /// Constructor for WalkthroughEffect. No scene graph modification is done here. The effect is not attached to the
    /// pane until [#attach(Node)] is called.
    ///
    /// @param pane The pane where the effect will be applied. Usually obtained from [Window#getScene()] and
    ///                         [Scene#getRoot()]
    protected BaseWindowEffect(@NonNull Pane pane) {
        this.pane = pane;
        this.debouncedUpdater = WalkthroughUtils.debounced(_ -> this.updateLayout());
    }

    /// The effect is no longer usable and permanently removed from the scene graph. To use this effect again, you can
    /// ONLY create a new instance of [BaseWindowEffect].
    public void detach() {
        hideEffect();
        cleanupListeners();
    }

    /// Updates the layout of the effect. This method is called whenever the pane or the target node (if any) changes
    /// its size or position.
    protected abstract void updateLayout();

    /// Hides the effect. The WalkthroughEffect can still be shown again after this method is called by calling
    /// [BaseWindowEffect#updateLayout()], either manually or due to a layout change in the pane or the target node.
    protected abstract void hideEffect();

    protected void cleanupListeners() {
        debouncedUpdater.cancel();
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }

    protected void setupListeners(@NonNull Node node) {
        subscriptions.add(EasyBind.listen(node.localToSceneTransformProperty(), debouncedUpdater));
        subscriptions.add(EasyBind.listen(node.boundsInLocalProperty(), debouncedUpdater));
        subscriptions.add(EasyBind.listen(new TreeShowingProperty(node), debouncedUpdater));

        Scene scene = node.getScene();
        if (scene != null) {
            subscriptions.add(EasyBind.listen(scene.widthProperty(), debouncedUpdater));
            subscriptions.add(EasyBind.listen(scene.heightProperty(), debouncedUpdater));
            setupWindowListeners(scene.getWindow());
        }

        subscriptions.add(EasyBind.listen(node.sceneProperty(), (_, _, newScene) -> {
            if (newScene != null) {
                subscriptions.add(EasyBind.listen(newScene.widthProperty(), debouncedUpdater));
                subscriptions.add(EasyBind.listen(newScene.heightProperty(), debouncedUpdater));
                setupWindowListeners(newScene.getWindow());
            }
        }));
    }

    protected void setupPaneListeners() {
        subscriptions.add(EasyBind.listen(pane.widthProperty(), debouncedUpdater));
        subscriptions.add(EasyBind.listen(pane.heightProperty(), debouncedUpdater));
        subscriptions.add(EasyBind.listen(pane.layoutBoundsProperty(), debouncedUpdater));
    }

    private void setupWindowListeners(@NonNull Window window) {
        subscriptions.add(EasyBind.listen(window.xProperty(), debouncedUpdater));
        subscriptions.add(EasyBind.listen(window.yProperty(), debouncedUpdater));
        subscriptions.add(EasyBind.listen(window.widthProperty(), debouncedUpdater));
        subscriptions.add(EasyBind.listen(window.heightProperty(), debouncedUpdater));
    }
}
