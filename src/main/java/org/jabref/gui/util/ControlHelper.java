package org.jabref.gui.util;

import java.util.function.Consumer;

import javax.swing.JComponent;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

public class ControlHelper {

    public static void setAction(ButtonType buttonType, DialogPane dialogPane, Consumer<Event> consumer) {
        Button button = (Button) dialogPane.lookupButton(buttonType);
        button.addEventFilter(ActionEvent.ACTION, (event -> {
            consumer.accept(event);
            event.consume();
        }));
    }

    public static void setSwingContent(DialogPane dialogPane, JComponent content) {
        SwingNode node = new SwingNode();
        node.setContent(content);
        node.setVisible(true);

        dialogPane.setContent(node);
    }
}
