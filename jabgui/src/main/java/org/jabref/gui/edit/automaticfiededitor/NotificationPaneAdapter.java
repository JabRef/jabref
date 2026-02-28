package org.jabref.gui.edit.automaticfiededitor;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.util.Duration;

import org.jabref.gui.icon.IconTheme;

import org.controlsfx.control.NotificationPane;

public class NotificationPaneAdapter extends NotificationPane {

    public NotificationPaneAdapter(Node content) {
        super(content);
    }

    public void notify(int affectedEntries, int totalEntries) {
        String text = "%d/%d affected entries".formatted(affectedEntries, totalEntries);
        Node graphic = IconTheme.JabRefIcons.INTEGRITY_INFO.getGraphicNode();

        this.setGraphic(graphic);
        this.setText(text);
        this.show();
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(_ -> hide());
        delay.play();
    }
}
