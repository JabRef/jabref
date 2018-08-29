package org.jabref.gui.util;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javax.swing.JComponent;

import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextFormatter;

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

    public static boolean childIsFocused(Parent node) {
        return node.isFocused() || node.getChildrenUnmodifiable().stream().anyMatch(child -> {
            if (child instanceof Parent) {
                return childIsFocused((Parent) child);
            } else {
                return child.isFocused();
            }
        });
    }

    /**
     * Returns a text formatter that restricts input to integers
     */
    public static TextFormatter<String> getIntegerTextFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getText();

            if (text.matches("[0-9]*")) {
                return change;
            }

            return null;
        };
        return new TextFormatter<>(filter);
    }
}
