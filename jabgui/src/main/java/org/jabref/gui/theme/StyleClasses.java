package org.jabref.gui.theme;

import java.util.List;

/// Utility-class combinations that recur across otherwise unrelated views. Referencing them
/// through a named constant keeps the semantic intent reviewable while the styling itself
/// stays on the shared utility classes.
public final class StyleClasses {

    /// Header above one side of a two-pane change/diff view.
    public static final List<String> CHANGE_VIEW_HEADER = List.of("h4", "padding-2");

    /// Explanatory legend below a change/diff view.
    public static final List<String> CHANGE_VIEW_LEGEND = List.of("font-size-090", "text-subtle", "padding-4");

    private StyleClasses() {
    }
}
