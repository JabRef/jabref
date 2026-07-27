package org.jabref.gui.preferences.forms;

/// The layout measurements a preference tab shares with {@link PreferencesFormBuilder}, so that a
/// tab building part of its view by hand (the `.custom(Node)` hatch) lines up with the parts the
/// builder laid out, rather than re-typing the numbers.
public final class FormMetrics {

    /// Gap between elements, both between rows and between an element and what is attached to it.
    public static final double GAP = 10.0;

    /// Width of a labelled button in a button row, so that the buttons in one row are uniform.
    public static final double BUTTON_WIDTH = 100.0;

    private FormMetrics() {
    }
}
