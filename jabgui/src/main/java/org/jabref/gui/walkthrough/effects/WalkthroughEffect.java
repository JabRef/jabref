package org.jabref.gui.walkthrough.effects;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.NonNull;

/// Base class for walkthrough effects BackdropHighlight, TooltipHighlight,
/// FullScreenDarken, etc.
public abstract class WalkthroughEffect {
    protected final Pane pane;
    /// list of subscriptions to scene graph changes, needs to be mutable
    private final List<Subscription> subscriptions = new ArrayList<>();

    /**
     * Constructor for WalkthroughEffect.
     *
     * @param pane The pane where the effect will be applied. Usually obtained from
     *             window.getScene().getRoot().
     */
    protected WalkthroughEffect(@NonNull Pane pane) {
        this.pane = pane;
        initializeEffect();
    }

    protected abstract void initializeEffect();

    protected abstract void updateLayout();

    /**
     * Hide the effect, e.g., by making it invisible. The effect should not be removed
     * from the pane, and the scene graph is not modified.
     */
    protected abstract void hideEffect();

    /**
     * Detach the effect, cleaning up any resources and listeners. The effect is no
     * longer active and reattaching will require scene graph modifications.
     */
    public void detach() {
        hideEffect();
        cleanupListeners();
    }

    private void cleanupListeners() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }

    protected void setupListeners(@NonNull Node node) {
        subscriptions.add(EasyBind.subscribe(node.localToSceneTransformProperty(), _ -> this.updateLayout()));

        Scene scene = node.getScene();
        if (scene != null) {
            subscriptions.add(EasyBind.subscribe(scene.widthProperty(), _ -> this.updateLayout()));
            subscriptions.add(EasyBind.subscribe(scene.heightProperty(), _ -> this.updateLayout()));
        }
    }
}
