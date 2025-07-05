package org.jabref.gui.walkthrough.effects;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Creates a full screen darken effect. Usually used to force user to ignore certain
 * window and focus on the modal.
 */
public class FullScreenDarken extends WalkthroughEffect {
    private static final Color OVERLAY_COLOR = Color.rgb(0, 0, 0, 0.55);

    private @Nullable Rectangle overlay;

    public FullScreenDarken(@NonNull Pane pane) {
        super(pane);
    }

    @Override
    protected void initializeEffect() {
        this.overlay = new Rectangle();
        this.overlay.setFill(OVERLAY_COLOR);
        this.overlay.setVisible(false);
        this.overlay.setManaged(false);
        this.pane.getChildren().add(overlay);
    }

    public void attach() {
        updater.cleanup();
        if (overlay == null) {
            initializeEffect();
        }
        setupPaneListeners();
        updateLayout();
    }

    @Override
    public void detach() {
        super.detach();
        assert overlay != null : "Run attach() before detach()";
        overlay.setVisible(false);
        pane.getChildren().remove(overlay);
        overlay = null;
    }

    @Override
    protected void updateLayout() {
        assert overlay != null : "Run attach() before updateLayout()";
        overlay.setX(0);
        overlay.setY(0);
        overlay.setWidth(pane.getWidth());
        overlay.setHeight(pane.getHeight());
        overlay.setVisible(true);
    }

    @Override
    protected void hideEffect() {
        assert overlay != null : "Run attach() before hideEffect()";
        overlay.setVisible(false);
    }
}
