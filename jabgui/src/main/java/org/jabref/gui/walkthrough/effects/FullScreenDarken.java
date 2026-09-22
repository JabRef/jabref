package org.jabref.gui.walkthrough.effects;

import javafx.event.Event;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class FullScreenDarken extends BaseWindowEffect {
    private @Nullable Rectangle overlay;

    public FullScreenDarken(@NonNull Pane pane) {
        super(pane);
    }

    public void attach() {
        if (overlay != null) {
            throw new IllegalStateException("FullScreenDarken is already attached. Detach it first.");
        }
        Rectangle overlay = new Rectangle();
        overlay.getStyleClass().add("walkthrough-darken");
        overlay.setVisible(false);
        overlay.setManaged(false);
        pane.getChildren().add(overlay);
        this.overlay = overlay;
        setupPaneListeners();
        updateLayout();
    }

    @Override
    public void detach() {
        if (overlay == null) {
            throw new IllegalStateException("FullScreenDarken is not attached.");
        }
        super.detach();
        overlay.setVisible(false);
        if (overlay.getParent() instanceof Pane parentPane) {
            parentPane.getChildren().remove(overlay);
        }
        overlay = null;
    }

    @Override
    protected void updateLayout() {
        if (overlay == null) {
            return;
        }

        overlay.setX(0);
        overlay.setY(0);
        overlay.setWidth(pane.getWidth());
        overlay.setHeight(pane.getHeight());

        // Clicks outside the target are swallowed so the user cannot interact with the shaded area
        overlay.setOnMouseClicked(Event::consume);

        overlay.setVisible(true);
    }

    @Override
    protected void hideEffect() {
        if (overlay != null) {
            overlay.setVisible(false);
        }
    }
}
