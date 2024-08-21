package org.jabref.gui.ai.components.notifications;

import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class NotificationsComponent extends Pane {
    public NotificationsComponent(List<Notification> notifications) {
        VBox vBox = new VBox(10);

        notifications
                .stream()
                .map(NotificationComponent::new)
                .forEach(vBox.getChildren()::add);

        ScrollPane scrollPane = new ScrollPane(vBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPadding(new Insets(10));

        getChildren().add(scrollPane);
    }
}
