package org.jabref.gui.walkthrough.effects;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import com.sun.javafx.scene.TreeShowingProperty;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.NonNull;

/// Base class for walkthrough effects [BackdropHighlight], [FullScreenDarken], and
/// [PulseAnimateIndicator].
public sealed abstract class BaseWindowEffect permits BackdropHighlight, FullScreenDarken, PulseAnimateIndicator {
    protected final Pane pane;
    protected final List<Subscription> subscriptions = new ArrayList<>();
    private ChangeListener<Number> windowSizeListener;

    /// Constructor for WalkthroughEffect. No scene graph modification is done here. The
    /// effect is not attached to the pane until [BaseWindowEffect#initializeEffect] is
    /// called. This only sets up the pane and prepares it for the effect to be attached
    /// later.
    ///
    /// @param pane The pane where the effect will be applied. Usually obtained from
    ///             [Window#getScene()] and [Scene#getRoot()]
    protected BaseWindowEffect(@NonNull Pane pane) {
        this.pane = pane;
        setupPaneListeners();
    }

    /// Attaches the effect to the pane. This method should be called before using the
    /// effect.
    protected abstract void initializeEffect();

    /// Updates the layout of the effect. This method is called whenever the pane or the
    /// target node (if any) changes its size or position.
    protected abstract void updateLayout();

    /// Hides the effect. The WalkthroughEffect can still be shown again after this
    /// method is called by calling [BaseWindowEffect#updateLayout()], either manually
    /// or due to a layout change in the pane or the target node.
    protected abstract void hideEffect();

    /// The effect is no longer usable and permanently removed from the scene graph. To
    /// use this effect again, you can ONLY create a new instance of
    /// [BaseWindowEffect].
    public void detach() {
        hideEffect();
        cleanupListeners();
        cleanupWindowListeners();
    }

    protected void cleanupListeners() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }

    protected void setupListeners(@NonNull Node node) {
        subscriptions.add(EasyBind.subscribe(node.localToSceneTransformProperty(), _ -> this.updateLayout()));
        subscriptions.add(EasyBind.subscribe(node.boundsInLocalProperty(), _ -> this.updateLayout()));
        subscriptions.add(EasyBind.subscribe(new TreeShowingProperty(node), _ -> this.updateLayout()));

        Scene scene = node.getScene();
        if (scene != null) {
            subscriptions.add(EasyBind.subscribe(scene.widthProperty(), _ -> this.updateLayout()));
            subscriptions.add(EasyBind.subscribe(scene.heightProperty(), _ -> this.updateLayout()));

            setupWindowListeners(scene.getWindow());
        }

        node.sceneProperty().addListener((_, oldScene, newScene) -> {
            if (oldScene != null && oldScene.getWindow() != null) {
                cleanupWindowListeners();
            }
            if (newScene != null) {
                subscriptions.add(EasyBind.subscribe(newScene.widthProperty(), _ -> this.updateLayout()));
                subscriptions.add(EasyBind.subscribe(newScene.heightProperty(), _ -> this.updateLayout()));
                setupWindowListeners(newScene.getWindow());
            }
        });
    }

    private void setupPaneListeners() {
        subscriptions.add(EasyBind.subscribe(pane.widthProperty(), _ -> this.updateLayout()));
        subscriptions.add(EasyBind.subscribe(pane.heightProperty(), _ -> this.updateLayout()));
        subscriptions.add(EasyBind.subscribe(pane.layoutBoundsProperty(), _ -> this.updateLayout()));
    }

    private void setupWindowListeners(Window window) {
        if (window != null) {
            windowSizeListener = (_, _, _) -> this.updateLayout();
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
