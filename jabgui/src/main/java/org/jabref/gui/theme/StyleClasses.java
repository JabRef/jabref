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

    /// Header introducing a section within a tab or form.
    public static final List<String> SECTION_HEADER = List.of("h4", "padding-top-12");

    /// Compact icon-only button, as used beside list/table editors.
    public static final List<String> NARROW_ICON_BUTTON = List.of("icon-button", "narrow");

    /// Header of a welcome-tab section (also used for the empty main-table placeholder).
    public static final List<String> WELCOME_HEADER = List.of("welcome-header-label", "h3", "bold");

    private StyleClasses() {
    }
}
