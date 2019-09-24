package org.jabref.gui.util;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Cell;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.DragEvent;

public class ControlHelper {

    // Pseudo-classes for drag and drop
    private static PseudoClass dragOverBottom = PseudoClass.getPseudoClass("dragOver-bottom");
    private static PseudoClass dragOverCenter = PseudoClass.getPseudoClass("dragOver-center");
    private static PseudoClass dragOverTop = PseudoClass.getPseudoClass("dragOver-top");

    public static void setAction(ButtonType buttonType, DialogPane dialogPane, Consumer<Event> consumer) {
        Button button = (Button) dialogPane.lookupButton(buttonType);
        button.addEventFilter(ActionEvent.ACTION, (event -> {
            consumer.accept(event);
            event.consume();
        }));
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

    public static void removePseudoClasses(Cell<?> cell, PseudoClass... pseudoClasses) {
        for (PseudoClass pseudoClass : pseudoClasses) {
            cell.pseudoClassStateChanged(pseudoClass, false);
        }
    }

    /**
     * Determines where the mouse is in the given cell.
     */
    public static DroppingMouseLocation getDroppingMouseLocation(Cell<?> cell, DragEvent event) {
        if ((cell.getHeight() * 0.25) > event.getY()) {
            return DroppingMouseLocation.TOP;
        } else if ((cell.getHeight() * 0.75) < event.getY()) {
            return DroppingMouseLocation.BOTTOM;
        } else {
            return DroppingMouseLocation.CENTER;
        }
    }

    public static void setDroppingPseudoClasses(Cell<?> cell, DragEvent event) {
        removeDroppingPseudoClasses(cell);
        switch (getDroppingMouseLocation(cell, event)) {
            case BOTTOM:
                cell.pseudoClassStateChanged(dragOverBottom, true);
                break;
            case CENTER:
                cell.pseudoClassStateChanged(dragOverCenter, true);
                break;
            case TOP:
                cell.pseudoClassStateChanged(dragOverTop, true);
                break;
        }
    }

    public static void setDroppingPseudoClasses(Cell<?> cell) {
        removeDroppingPseudoClasses(cell);
        cell.pseudoClassStateChanged(dragOverCenter, true);
    }

    public static void removeDroppingPseudoClasses(Cell<?> cell) {
        removePseudoClasses(cell, dragOverBottom, dragOverCenter, dragOverTop);
    }
}
