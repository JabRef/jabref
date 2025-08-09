package org.jabref.gui.walkthrough.declarative.step;

/// The position of the quit button in the walkthrough.
public enum QuitButtonPosition {
    /// Bottom right corner - good for most panel positions
    BOTTOM_RIGHT,

    /// Top right corner - good when panel is at bottom
    TOP_RIGHT,

    /// Bottom left corner - good when panel is on the right
    BOTTOM_LEFT,

    /// Top left corner - good when panel is at bottom right
    TOP_LEFT,

    /// Automatically position based on panel position to avoid overlap
    AUTO
}
