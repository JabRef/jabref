package org.jabref.gui.walkthrough.effects;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class FullScreenDarken extends BaseWindowEffect {
    private static final Color OVERLAY_COLOR = Color.rgb(0, 0, 0, 0.55);

    private @Nullable Rectangle overlay;
    private @Nullable Runnable onClickHandler;

    public FullScreenDarken(@NonNull Pane pane) {
        super(pane);
    }

    public void setOnClick(@Nullable Runnable onClickHandler) {
        this.onClickHandler = onClickHandler;
    }

    @Override
    protected void initializeEffect() {
        this.overlay = new Rectangle();
        this.overlay.setFill(OVERLAY_COLOR);
        this.overlay.setVisible(false);
        this.overlay.setManaged(false);
        getOrAddToPane();
    }

    public void attach() {
        if (overlay == null) {
            initializeEffect();
        }
        updateLayout();
    }

    @Override
    public void detach() {
        super.detach();
        if (overlay != null) {
            overlay.setVisible(false);
            if (overlay.getParent() instanceof Pane parentPane) {
                parentPane.getChildren().remove(overlay);
            }
            overlay = null;
        }
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

        if (onClickHandler != null) {
            overlay.setOnMouseClicked(this::handleClick);
            overlay.setMouseTransparent(false);
        } else {
            overlay.setMouseTransparent(true);
        }

        overlay.setVisible(true);
    }

    @Override
    protected void hideEffect() {
        if (overlay != null) {
            overlay.setVisible(false);
        }
    }

    private void getOrAddToPane() {
        if (overlay != null && !pane.getChildren().contains(overlay)) {
            pane.getChildren().add(overlay);
        }
    }

    private void handleClick(MouseEvent event) {
        if (onClickHandler != null) {
            onClickHandler.run();
        }
        event.consume();
    }
}
