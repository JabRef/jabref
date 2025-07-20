package org.jabref.gui.welcome.components;

import javafx.beans.InvalidationListener;
import javafx.collections.ObservableSet;
import javafx.scene.control.ListCell;

import org.jabref.gui.push.GuiPushToApplication;

public class PushToApplicationCell extends ListCell<GuiPushToApplication> {
    public static final String DETECTED_APPLICATION_STYLE_CLASS = "detected-application";

    private final ObservableSet<GuiPushToApplication> detectedApplications;

    public PushToApplicationCell(ObservableSet<GuiPushToApplication> detectedApplications) {
        this.detectedApplications = detectedApplications;
        this.getStyleClass().add("application-item");

        InvalidationListener listener = _ -> updateDetectionStyle();
        this.itemProperty().addListener(listener);
        this.detectedApplications.addListener(listener);
    }

    @Override
    protected void updateItem(GuiPushToApplication application, boolean empty) {
        super.updateItem(application, empty);

        if (empty || application == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().removeAll(DETECTED_APPLICATION_STYLE_CLASS);
            return;
        }

        setText(application.getDisplayName());
        setGraphic(application.getApplicationIcon().getGraphicNode());
        updateDetectionStyle();
    }

    private void updateDetectionStyle() {
        GuiPushToApplication item = getItem();
        boolean isDetected = item != null && detectedApplications.contains(item);

        if (isDetected) {
            if (!getStyleClass().contains(DETECTED_APPLICATION_STYLE_CLASS)) {
                getStyleClass().add(DETECTED_APPLICATION_STYLE_CLASS);
            }
        } else {
            getStyleClass().removeAll(DETECTED_APPLICATION_STYLE_CLASS);
        }
    }
}
