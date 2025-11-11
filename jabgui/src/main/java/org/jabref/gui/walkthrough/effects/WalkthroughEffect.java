package org.jabref.gui.walkthrough.effects;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

import org.jabref.gui.walkthrough.WalkthroughUpdater;

import org.jspecify.annotations.NonNull;

/// Base class for walkthrough effects BackdropHighlight, TooltipHighlight, FullScreenDarken, etc.
public abstract class WalkthroughEffect {
    protected final Pane pane;
    protected final WalkthroughUpdater updater = new WalkthroughUpdater();

    /**
     * Constructor for WalkthroughEffect.
     *
     * @param pane The pane where the effect will be applied. Usually obtained from window.getScene().getRoot().
     */
    protected WalkthroughEffect(@NonNull Pane pane) {
        this.pane = pane;
        initializeEffect();
    }

    protected abstract void initializeEffect();

    protected abstract void updateLayout();

    /**
     * Hide the effect, e.g., by making it invisible. The effect should not be removed from the pane,
     * and the scene graph is not modified.
     */
    protected abstract void hideEffect();

    /**
     * Detach the effect, cleaning up any resources and listeners. The effect is no longer active
     * and reattaching will require scene graph modifications.
     */
    public void detach() {
        updater.cleanup();
        hideEffect();
    }

    protected void setupNodeListeners(@NonNull Node node) {
        updater.setupNodeListeners(node, this::updateLayout);
    }

    protected void setupPaneListeners() {
        updater.listen(pane.widthProperty(), _ -> updateLayout());
        updater.listen(pane.heightProperty(), _ -> updateLayout());
        updater.listen(pane.sceneProperty(), (_, _, newScene) -> {
            updateLayout();
            if (newScene == null) {
                return;
            }
            updater.listen(newScene.heightProperty(), _ -> updateLayout());
            updater.listen(newScene.widthProperty(), _ -> updateLayout());
            if (newScene.getWindow() != null) {
                updater.setupWindowListeners(newScene.getWindow(), this::updateLayout);
            }
        });
    }
}
