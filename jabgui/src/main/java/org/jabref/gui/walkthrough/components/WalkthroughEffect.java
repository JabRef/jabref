package org.jabref.gui.walkthrough.components;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

import org.jabref.gui.walkthrough.WalkthroughUpdater;

import org.jspecify.annotations.NonNull;

/**
 * Base class for walkthrough effects with common listener management and positioning.
 */
public abstract class WalkthroughEffect {
    protected final Pane pane;
    protected final WalkthroughUpdater updater = new WalkthroughUpdater();

    protected WalkthroughEffect(@NonNull Pane pane) {
        this.pane = pane;
        initializeEffect();
    }

    protected abstract void initializeEffect();

    protected abstract void updateLayout();

    protected abstract void hideEffect();

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
