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

    public enum EllipsisPosition { BEGINNING, CENTER, ENDING }

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

    /**
     * If needed, truncates a given string to <code>maxCharacters</code>, adding <code>ellipsisString</code> instead.
     *
     * @param text text which should be truncated, if needed
     * @param maxCharacters maximum amount of characters which the resulting text should have, including the
     *                      <code>ellipsisString</code>; if set to -1, then the default length of 75 characters will be
     *                      used
     * @param ellipsisString string which should be used for indicating the truncation
     * @param ellipsisPosition location in the given text where the truncation should be performed
     * @return the new, truncated string
     */
    public static String truncateString(String text, int maxCharacters, String ellipsisString, EllipsisPosition ellipsisPosition) {
        if (text == null || "".equals(text)) {
            return text; // return original
        }

        if (ellipsisString == null) {
            ellipsisString = "";
        }

        if (maxCharacters == -1) {
            maxCharacters = 75; // default
        }

        maxCharacters = Math.max(ellipsisString.length(), maxCharacters);

        if (text.length() > maxCharacters) {
            // truncation necessary
            switch (ellipsisPosition) {
                case BEGINNING:
                    return ellipsisString + text.substring(text.length() - (maxCharacters - ellipsisString.length()));
                case CENTER:
                    int partialLength = (int) Math.floor((maxCharacters - ellipsisString.length()) / 2f);
                    return text.substring(0, partialLength) + ellipsisString + text.substring(text.length() - partialLength);
                case ENDING:
                    return text.substring(0, maxCharacters - ellipsisString.length()) + ellipsisString;
            }
        }

        return text;
    }
}
