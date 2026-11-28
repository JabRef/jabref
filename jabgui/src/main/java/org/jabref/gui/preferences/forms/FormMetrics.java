package org.jabref.gui.preferences.forms;

import org.jspecify.annotations.NullMarked;

/// The layout measurements a preference tab shares with [PreferencesFormBuilder], so that a
/// tab building part of its view by hand (the `.custom(Node)` hatch) lines up with the parts the
/// builder laid out, rather than re-typing the numbers.
@NullMarked
public final class FormMetrics {

    /// Gap between elements, both between rows and between an element and what is attached to it.
    public static final double GAP = 8.0;

    /// Width of a labelled button in a button row, so that the buttons in one row are uniform.
    public static final double BUTTON_WIDTH = 100.0;
    static final double SHORT_FIELD_WIDTH = 100.0;

    /// Gap between a tile's caption and its control.
    static final double TILE_HEADER_GAP = 16.0;

    /// Width of a combo box in a tile, so that the drop-downs of the dialog are alike.
    static final double TILE_CONTROL_WIDTH = 240.0;

    /// Width of a tile's caption column: the controls of all tiles start where it ends. Longer
    /// captions wrap.
    static final double TILE_CAPTION_COLUMN_WIDTH = 240.0;

    static final double INFO_LABEL_INDENT = 20.0;

    static final double ICON_BUTTON_SIZE = 20.0;

    private FormMetrics() {
    }
}
