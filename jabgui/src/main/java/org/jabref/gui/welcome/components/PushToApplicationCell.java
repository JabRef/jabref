package org.jabref.gui.welcome.components;

import java.util.Set;

import javafx.scene.control.ListCell;

import org.jabref.gui.push.GuiPushToApplication;

public class PushToApplicationCell extends ListCell<GuiPushToApplication> {
    private final Set<GuiPushToApplication> detectedApplications;

    public PushToApplicationCell(Set<GuiPushToApplication> detectedApplications) {
        this.detectedApplications = detectedApplications;
        this.getStyleClass().add("application-item");
    }

    @Override
    protected void updateItem(GuiPushToApplication application, boolean empty) {
        super.updateItem(application, empty);
        if (empty || application == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().removeAll("detected-application");
            return;
        }
        setText(application.getDisplayName());
        setGraphic(application.getApplicationIcon().getGraphicNode());
        if (detectedApplications.contains(application)) {
            if (!getStyleClass().contains("detected-application")) {
                getStyleClass().add("detected-application");
            }
        } else {
            getStyleClass().removeAll("detected-application");
        }
    }
}
