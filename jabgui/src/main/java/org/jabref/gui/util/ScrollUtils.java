package org.jabref.gui.util;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ScrollUtils {

    private ScrollUtils() {
    }

    public static void scrollIntoScrollPane(ScrollPane scrollPane, Bounds targetBounds) {
        Node content = scrollPane.getContent();
        if (content == null) {
            return;
        }

        Bounds contentBounds = content.getBoundsInLocal();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        if (contentBounds.getHeight() <= viewportHeight) {
            return;
        }

        Bounds targetInContent = content.sceneToLocal(targetBounds);
        double maxScrollY = contentBounds.getHeight() - viewportHeight;
        double desiredScrollY = targetInContent.getCenterY() - (viewportHeight / 2);

        scrollPane.setVvalue(Math.clamp(desiredScrollY / maxScrollY, 0, 1));
    }
}
