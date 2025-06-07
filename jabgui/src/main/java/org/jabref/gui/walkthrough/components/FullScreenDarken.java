package org.jabref.gui.walkthrough.components;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.jspecify.annotations.NonNull;

/**
 * Creates a full screen darken effect. Usually used to force user to ignore certain
 * window and focus on the modal.
 */
public class FullScreenDarken extends WalkthroughEffect {
    private static final Color OVERLAY_COLOR = Color.rgb(0, 0, 0, 0.55);

    private Rectangle overlay;

    public FullScreenDarken(@NonNull Pane pane) {
        super(pane);
    }

    @Override
    protected void initializeEffect() {
        this.overlay = new Rectangle();
        this.overlay.setFill(OVERLAY_COLOR);
        this.overlay.setVisible(false);
        this.pane.getChildren().add(overlay);
    }

    /**
     * Attaches the effect to the pane
     */
    public void attach() {
        cleanUp();
        if (overlay == null) {
            initializeEffect();
        }
        setupPaneListeners();
        updateLayout();
    }

    @Override
    public void detach() {
        if (overlay != null && overlay.getParent() != null) {
            overlay.setVisible(false);
            pane.getChildren().remove(overlay);
        }
        super.detach();
    }

    @Override
    protected void updateLayout() {
        overlay.setX(0);
        overlay.setY(0);
        overlay.setWidth(pane.getWidth());
        overlay.setHeight(pane.getHeight());
        overlay.setVisible(true);
    }

    @Override
    protected void hideEffect() {
        overlay.setVisible(false);
    }
}
