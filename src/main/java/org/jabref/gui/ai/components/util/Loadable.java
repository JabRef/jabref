package org.jabref.gui.ai.components.util;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class Loadable extends StackPane {
    private boolean isInLoadingState = false;

    public void setLoading(boolean loading) {
        if (loading == isInLoadingState) {
            return;
        }

        if (loading) {
            getChildren().add(new BorderPane(new ProgressIndicator()));
        } else {
            getChildren().removeLast();
        }

        isInLoadingState = loading;
    }
}
