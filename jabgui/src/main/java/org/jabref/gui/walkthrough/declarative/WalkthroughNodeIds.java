package org.jabref.gui.walkthrough.declarative;

import org.jspecify.annotations.NullMarked;

/// Every node id a walkthrough step spots on.
///
/// A node id is how a step finds the control it wants to highlight. Collecting the ids here makes
/// them reusable, shows at a glance which controls the walkthroughs depend on, and keeps one from
/// being deleted by accident — renaming an id silently breaks the step that looked for it.
///
/// Styling goes through style classes, so that renaming an id never changes the look and restyling
/// never breaks a walkthrough. Ids on nodes built in Java are set from these constants, which makes
/// the dependency visible at the definition. FXML cannot reference a constant, so those ids repeat
/// the literal in the FXML file named below; `WalkthroughNodeIdsTest` pins every constant to its
/// declaration and guards that no stylesheet selects by one of them.
@NullMarked
public final class WalkthroughNodeIds {

    /// The main entry table, set in `MainTable`.
    public static final String MAIN_TABLE = "main-table";

    /// The groups panel, set in `GroupsSidePaneComponent`.
    public static final String GROUPS_SIDE_PANE = "groups-side-pane";

    /// Search field of the global search bar, set in `GlobalSearchBar`. The bar's own id, not the
    /// one `SearchTextField` gives every field it builds — the web search pane carries that too.
    public static final String GLOBAL_SEARCH_FIELD = "global-search-field";

    /// Column table of the "Entry table" preferences tab, set in `TableTab`.
    public static final String COLUMNS_LIST = "columns-list";

    /// "Main file directory" radio of the "Linked files" preferences tab, set in `LinkedFilesTab`.
    public static final String MAIN_FILE_DIRECTORY_RADIO = "main-file-directory";

    /// "Browse" button of `LinkedFileEditDialog.fxml`.
    public static final String LINKED_FILE_BROWSE = "browse";

    /// Description field of `LinkedFileEditDialog.fxml`.
    public static final String LINKED_FILE_DESCRIPTION = "description";

    /// File type combo of `LinkedFileEditDialog.fxml`.
    public static final String LINKED_FILE_TYPE = "fileType";

    /// Source URL field of `LinkedFileEditDialog.fxml`.
    public static final String LINKED_FILE_SOURCE_URL = "sourceUrl";

    /// Group name field of `GroupDialog.fxml`.
    public static final String GROUP_NAME = "nameField";

    /// Group description field of `GroupDialog.fxml`.
    public static final String GROUP_DESCRIPTION = "descriptionField";

    /// "Explicit selection" radio of `GroupDialog.fxml`.
    public static final String GROUP_EXPLICIT_RADIO = "explicitRadioButton";

    private WalkthroughNodeIds() {
    }
}
