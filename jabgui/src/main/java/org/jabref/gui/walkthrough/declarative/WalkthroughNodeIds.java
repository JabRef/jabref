package org.jabref.gui.walkthrough.declarative;

/// Every node id a walkthrough step spots on.
///
/// A node id exists for the walkthrough alone: it is how a step finds the control it wants to
/// highlight. Nothing styles by id — the stylesheets address controls through style classes —
/// so an id that is not listed here has no reason to exist, and renaming one silently breaks
/// the step that looked for it.
///
/// Ids on nodes built in Java are set from these constants, which makes the dependency visible
/// at the definition. FXML cannot reference a constant, so those ids repeat the literal in the
/// FXML file named below; `WalkthroughNodeIdsTest` pins every constant to its declaration and
/// guards the rule that no stylesheet selects by id.
public final class WalkthroughNodeIds {

    /// Column table of the "Entry table" preferences tab, set in `TableTab`.
    public static final String COLUMNS_LIST = "columnsList";

    /// "Main file directory" radio of the "Linked files" preferences tab, set in `LinkedFilesTab`.
    public static final String MAIN_FILE_DIRECTORY_RADIO = "useMainFileDirectory";

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
