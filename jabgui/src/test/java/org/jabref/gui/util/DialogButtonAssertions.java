package org.jabref.gui.util;

import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.text.Text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Assertions on the buttons of a [DialogPane], to be used from a JavaFX thread.
public class DialogButtonAssertions {

    private DialogButtonAssertions() {
    }

    private static List<Button> buttonsOf(DialogPane pane) {
        return pane.getButtonTypes().stream()
                   .map(pane::lookupButton)
                   .map(Button.class::cast)
                   .toList();
    }

    /// Asserts that every button of the pane's button bar shows its whole caption.
    public static void assertCaptionsAreNotTruncated(DialogPane pane) {
        assertCaptionsAreNotTruncated(buttonsOf(pane));
    }

    /// Asserts that every one of the buttons shows its whole caption.
    public static void assertCaptionsAreNotTruncated(List<Button> buttons) {
        for (Button button : buttons) {
            // A caption that does not fit is rendered with an ellipsis instead of the caption itself.
            assertEquals(button.getText(), ((Text) button.lookup(".text")).getText(),
                    "Caption of button '%s' is truncated".formatted(button.getText()));

            // Even without an ellipsis, the button must be wide enough for the caption in the font it is rendered in.
            double requiredWidth = requiredWidth(button);
            assertTrue(button.getWidth() >= requiredWidth,
                    "Button '%s' is %.1fpx wide, but its caption needs %.1fpx".formatted(button.getText(), button.getWidth(), requiredWidth));
        }
    }

    /// The width the button needs to show its whole caption: the caption measured in the font of the button plus the space its background and padding take.
    private static double requiredWidth(Button button) {
        Text caption = new Text(button.getText());
        caption.setFont(button.getFont());
        Insets insets = button.getInsets();
        return caption.getLayoutBounds().getWidth() + insets.getLeft() + insets.getRight();
    }
}
