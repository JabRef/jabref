package org.jabref.gui.keyboard;

import org.jabref.logic.l10n.Localization;

public enum KeyBinding {
    EDITOR_DELETE("Delete", Localization.lang("Delete text"), "", KeyBindingCategory.EDITOR),
    // DELETE BACKWARDS = Rubout
    EDITOR_BACKWARD("Move caret left", Localization.lang("Move caret left"), "", KeyBindingCategory.EDITOR),
    EDITOR_FORWARD("Move caret right", Localization.lang("Move caret right"), "", KeyBindingCategory.EDITOR),
    EDITOR_WORD_BACKWARD("Move caret to previous word", Localization.lang("Move caret to previous word"), "", KeyBindingCategory.EDITOR),
    EDITOR_WORD_FORWARD("Move caret to next word", Localization.lang("Move caret to next word"), "", KeyBindingCategory.EDITOR),
    EDITOR_BEGINNING("Move caret to beginning of line", Localization.lang("Move caret to beginning of line"), "", KeyBindingCategory.EDITOR),
    EDITOR_END("Move caret to  of line", Localization.lang("Move caret to end of line"), "", KeyBindingCategory.EDITOR),
    EDITOR_BEGINNING_DOC("Move caret to beginning of text", Localization.lang("Move the caret to the beginning of text"), "", KeyBindingCategory.EDITOR),
    EDITOR_END_DOC("Move caret to end of text", Localization.lang("Move the caret to the end of text"), "", KeyBindingCategory.EDITOR),
    EDITOR_UP("Move caret up", Localization.lang("Move the caret up"), "", KeyBindingCategory.EDITOR),
    EDITOR_DOWN("Move caret down", Localization.lang("Move the caret down"), "", KeyBindingCategory.EDITOR),
    EDITOR_CAPITALIZE("Capitalize word", Localization.lang("Capitalize current word"), "", KeyBindingCategory.EDITOR),
    EDITOR_LOWERCASE("Lowercase word", Localization.lang("Make current word lowercase"), "", KeyBindingCategory.EDITOR),
    EDITOR_UPPERCASE("Uppercase word", Localization.lang("Make current word uppercase"), "", KeyBindingCategory.EDITOR),
    EDITOR_KILL_LINE("Remove all characters caret to end of line", Localization.lang("Remove line after caret"), "", KeyBindingCategory.EDITOR),
    EDITOR_KILL_WORD("Remove characters until next word", Localization.lang("Remove characters until next word"), "", KeyBindingCategory.EDITOR),
    EDITOR_KILL_WORD_BACKWARD("Characters until previous word", Localization.lang("Remove the current word backwards"), "", KeyBindingCategory.EDITOR),

    ABBREVIATE("Abbreviate", Localization.lang("Abbreviate journal names"), "ctrl+alt+A", KeyBindingCategory.TOOLS),
    AUTOGENERATE_CITATION_KEYS("Autogenerate citation keys", Localization.lang("Autogenerate citation keys"), "ctrl+G", KeyBindingCategory.QUALITY),
    ACCEPT("Accept", Localization.lang("Accept"), "ctrl+ENTER", KeyBindingCategory.EDIT),
    AUTOMATICALLY_LINK_FILES("Automatically link files", Localization.lang("Automatically set file links"), "F7", KeyBindingCategory.QUALITY),
    CHECK_INTEGRITY("Check integrity", Localization.lang("Check integrity"), "ctrl+F8", KeyBindingCategory.QUALITY),
    CLEANUP("Cleanup", Localization.lang("Cleanup entries"), "alt+F8", KeyBindingCategory.QUALITY),
    CLOSE_DATABASE("Close library", Localization.lang("Close library"), "ctrl+W", KeyBindingCategory.FILE),
    CLOSE("Close dialog", Localization.lang("Close dialog"), "Esc", KeyBindingCategory.VIEW),
    COPY("Copy", Localization.lang("Copy"), "ctrl+C", KeyBindingCategory.EDIT),
    COPY_TITLE("Copy title", Localization.lang("Copy title"), "ctrl+shift+alt+T", KeyBindingCategory.EDIT),
    COPY_CITE_CITATION_KEY("Copy \\cite{citation key}", Localization.lang("Copy \\cite{citation key}"), "ctrl+K", KeyBindingCategory.EDIT),
    COPY_CITATION_KEY("Copy citation key", Localization.lang("Copy citation key"), "ctrl+shift+K", KeyBindingCategory.EDIT),
    COPY_CITATION_KEY_AND_TITLE("Copy citation key and title", Localization.lang("Copy citation key and title"), "ctrl+shift+alt+K", KeyBindingCategory.EDIT),
    COPY_CITATION_KEY_AND_LINK("Copy citation key and link", Localization.lang("Copy citation key and link"), "ctrl+alt+K", KeyBindingCategory.EDIT),
    COPY_PREVIEW("Copy preview", Localization.lang("Copy preview"), "ctrl+shift+C", KeyBindingCategory.VIEW),
    CUT("Cut", Localization.lang("Cut"), "ctrl+X", KeyBindingCategory.EDIT),
    // We have to put Entry Editor Previous before, because otherwise the decrease font size is found first
    ENTRY_EDITOR_PREVIOUS_PANEL_2("Entry editor, previous panel 2", Localization.lang("Entry editor, previous panel 2"), "ctrl+MINUS", KeyBindingCategory.VIEW),
    DELETE_ENTRY("Delete entry", Localization.lang("Delete entry"), "DELETE", KeyBindingCategory.BIBTEX),
    DEFAULT_DIALOG_ACTION("Execute default action in dialog", Localization.lang("Execute default action in dialog"), "ctrl+ENTER", KeyBindingCategory.VIEW),
    DOWNLOAD_FULL_TEXT("Download full text documents", Localization.lang("Download full text documents"), "alt+F7", KeyBindingCategory.QUALITY),
    EDIT_ENTRY("Open / close entry editor", Localization.lang("Open / close entry editor"), "ctrl+E", KeyBindingCategory.VIEW),
    EXPORT("Export", Localization.lang("Export"), "ctrl+alt+e", KeyBindingCategory.FILE),
    EXPORT_SELECTED("Export Selected", Localization.lang("Export selected entries"), "ctrl+shift+e", KeyBindingCategory.FILE),
    EDIT_STRINGS("Edit strings", Localization.lang("Edit strings"), "ctrl+T", KeyBindingCategory.BIBTEX),
    ENTRY_EDITOR_NEXT_ENTRY("Entry editor, next entry", Localization.lang("Entry editor, next entry"), "alt+DOWN", KeyBindingCategory.VIEW),
    ENTRY_EDITOR_NEXT_PANEL("Entry editor, next panel", Localization.lang("Entry editor, next panel"), "ctrl+TAB", KeyBindingCategory.VIEW),
    ENTRY_EDITOR_NEXT_PANEL_2("Entry editor, next panel 2", Localization.lang("Entry editor, next panel 2"), "ctrl+PLUS", KeyBindingCategory.VIEW),
    ENTRY_EDITOR_PREVIOUS_ENTRY("Entry editor, previous entry", Localization.lang("Entry editor, previous entry"), "alt+UP", KeyBindingCategory.VIEW),
    ENTRY_EDITOR_PREVIOUS_PANEL("Entry editor, previous panel", Localization.lang("Entry editor, previous panel"), "ctrl+shift+TAB", KeyBindingCategory.VIEW),
    FILE_LIST_EDITOR_MOVE_ENTRY_DOWN("File list editor, move entry down", Localization.lang("File list editor, move entry down"), "ctrl+DOWN", KeyBindingCategory.VIEW),
    FILE_LIST_EDITOR_MOVE_ENTRY_UP("File list editor, move entry up", Localization.lang("File list editor, move entry up"), "ctrl+UP", KeyBindingCategory.VIEW),
    FIND_UNLINKED_FILES("Search for unlinked local files", Localization.lang("Search for unlinked local files"), "shift+F7", KeyBindingCategory.QUALITY),
    FOCUS_ENTRY_TABLE("Focus entry table", Localization.lang("Focus entry table"), "alt+1", KeyBindingCategory.VIEW),
    HELP("Help", Localization.lang("Help"), "F1", KeyBindingCategory.FILE),
    IMPORT_INTO_CURRENT_DATABASE("Import into current library", Localization.lang("Import into current library"), "ctrl+I", KeyBindingCategory.FILE),
    IMPORT_INTO_NEW_DATABASE("Import into new library", Localization.lang("Import into new library"), "ctrl+alt+I", KeyBindingCategory.FILE),
    MERGE_ENTRIES("Merge entries", Localization.lang("Merge entries"), "ctrl+M", KeyBindingCategory.TOOLS),

    NEW_ARTICLE("New article", Localization.lang("New article"), "ctrl+shift+A", KeyBindingCategory.BIBTEX),
    NEW_BOOK("New book", Localization.lang("New book"), "ctrl+shift+B", KeyBindingCategory.BIBTEX),
    NEW_ENTRY("New entry", Localization.lang("New entry"), "ctrl+N", KeyBindingCategory.BIBTEX),
    NEW_ENTRY_FROM_PLAIN_TEXT("New entry from plain text", Localization.lang("New entry from plain text"), "ctrl+shift+N", KeyBindingCategory.BIBTEX),
    NEW_INBOOK("New inbook", Localization.lang("New inbook"), "ctrl+shift+I", KeyBindingCategory.BIBTEX),
    NEW_MASTERSTHESIS("New mastersthesis", Localization.lang("New mastersthesis"), "ctrl+shift+M", KeyBindingCategory.BIBTEX),
    NEW_PHDTHESIS("New phdthesis", Localization.lang("New phdthesis"), "ctrl+shift+T", KeyBindingCategory.BIBTEX),
    NEW_PROCEEDINGS("New proceedings", Localization.lang("New proceedings"), "ctrl+shift+P", KeyBindingCategory.BIBTEX),
    NEW_UNPUBLISHED("New unpublished", Localization.lang("New unpublished"), "ctrl+shift+U", KeyBindingCategory.BIBTEX),
    NEW_TECHREPORT("New technical report", Localization.lang("New technical report"), "", KeyBindingCategory.BIBTEX),
    NEW_INPROCEEDINGS("New inproceesings", Localization.lang("New inproceedings"), "", KeyBindingCategory.BIBTEX),

    NEXT_PREVIEW_LAYOUT("Next preview layout", Localization.lang("Next preview layout"), "F9", KeyBindingCategory.VIEW),
    NEXT_LIBRARY("Next library", Localization.lang("Next library"), "ctrl+PAGE_DOWN", KeyBindingCategory.VIEW),
    OPEN_CONSOLE("Open terminal here", Localization.lang("Open terminal here"), "ctrl+shift+L", KeyBindingCategory.TOOLS),
    OPEN_DATABASE("Open library", Localization.lang("Open library"), "ctrl+O", KeyBindingCategory.FILE),
    OPEN_FILE("Open file", Localization.lang("Open file"), "F4", KeyBindingCategory.TOOLS),
    OPEN_FOLDER("Open folder", Localization.lang("Open folder"), "ctrl+shift+O", KeyBindingCategory.TOOLS),
    OPEN_OPEN_OFFICE_LIBRE_OFFICE_CONNECTION("Open OpenOffice/LibreOffice connection", Localization.lang("Open OpenOffice/LibreOffice connection"), "alt+0", KeyBindingCategory.TOOLS),
    OPEN_URL_OR_DOI("Open URL or DOI", Localization.lang("Open URL or DOI"), "F3", KeyBindingCategory.TOOLS),
    PASTE("Paste", Localization.lang("Paste"), "ctrl+V", KeyBindingCategory.EDIT),
    PULL_CHANGES_FROM_SHARED_DATABASE("Pull changes from shared database", Localization.lang("Pull changes from shared database"), "ctrl+shift+R", KeyBindingCategory.FILE),
    PREAMBLE_EDITOR_STORE_CHANGES("Preamble editor, store changes", Localization.lang("Preamble editor, store changes"), "alt+S", KeyBindingCategory.FILE),
    PREVIOUS_PREVIEW_LAYOUT("Previous preview layout", Localization.lang("Previous preview layout"), "shift+F9", KeyBindingCategory.VIEW),
    PREVIOUS_LIBRARY("Previous library", Localization.lang("Previous library"), "ctrl+PAGE_UP", KeyBindingCategory.VIEW),
    PUSH_TO_APPLICATION("Push to application", Localization.lang("Push to application"), "ctrl+L", KeyBindingCategory.TOOLS),
    QUIT_JABREF("Quit JabRef", Localization.lang("Quit JabRef"), "ctrl+Q", KeyBindingCategory.FILE),
    REDO("Redo", Localization.lang("Redo"), "ctrl+Y", KeyBindingCategory.EDIT),
    REFRESH_OO("Refresh OO", Localization.lang("Refresh OpenOffice/LibreOffice"), "ctrl+alt+O", KeyBindingCategory.TOOLS),
    REPLACE_STRING("Replace string", Localization.lang("Replace string"), "ctrl+R", KeyBindingCategory.SEARCH),
    RESOLVE_DUPLICATE_CITATION_KEYS("Resolve duplicate citation keys", Localization.lang("Resolve duplicate citation keys"), "ctrl+shift+D", KeyBindingCategory.BIBTEX),
    SAVE_ALL("Save all", Localization.lang("Save all"), "ctrl+alt+S", KeyBindingCategory.FILE),
    SAVE_DATABASE("Save library", Localization.lang("Save library"), "ctrl+S", KeyBindingCategory.FILE),
    SAVE_DATABASE_AS("Save library as ...", Localization.lang("Save library as..."), "ctrl+shift+S", KeyBindingCategory.FILE),
    SEARCH("Search", Localization.lang("Search"), "ctrl+F", KeyBindingCategory.SEARCH),
    SELECT_ALL("Select all", Localization.lang("Select all"), "ctrl+A", KeyBindingCategory.EDIT),
    SELECT_FIRST_ENTRY("Select first entry", Localization.lang("Select first entry"), "HOME", KeyBindingCategory.EDIT),
    SELECT_LAST_ENTRY("Select last entry", Localization.lang("Select last entry"), "END", KeyBindingCategory.EDIT),
    STRING_DIALOG_ADD_STRING("String dialog, add string", Localization.lang("String dialog, add string"), "ctrl+N", KeyBindingCategory.FILE),
    STRING_DIALOG_REMOVE_STRING("String dialog, remove string", Localization.lang("String dialog, remove string"), "shift+DELETE", KeyBindingCategory.FILE),
    SYNCHRONIZE_FILES("Synchronize files", Localization.lang("Synchronize files"), "ctrl+shift+F7", KeyBindingCategory.QUALITY),
    TOGGLE_GROUPS_INTERFACE("Toggle groups interface", Localization.lang("Toggle groups interface"), "alt+3", KeyBindingCategory.VIEW),
    UNABBREVIATE("Unabbreviate", Localization.lang("Unabbreviate"), "ctrl+alt+shift+A", KeyBindingCategory.TOOLS),
    UNDO("Undo", Localization.lang("Undo"), "ctrl+Z", KeyBindingCategory.EDIT),
    WEB_SEARCH("Web search", Localization.lang("Web search"), "alt+4", KeyBindingCategory.SEARCH),
    WRITE_METADATA_TO_PDF("Write metadata to PDF files", Localization.lang("Write metadata to PDF files"), "F6", KeyBindingCategory.TOOLS),
    CLEAR_SEARCH("Clear search", Localization.lang("Clear search"), "ESCAPE", KeyBindingCategory.SEARCH),
    CLEAR_READ_STATUS("Clear read status", Localization.lang("Clear read status"), "", KeyBindingCategory.EDIT),
    READ("Set read status to read", Localization.lang("Set read status to read"), "", KeyBindingCategory.EDIT),
    SKIMMED("Set read status to skimmed", Localization.lang("Set read status to skimmed"), "", KeyBindingCategory.EDIT);

    private final String constant;
    private final String localization;
    private final String defaultBinding;
    private final KeyBindingCategory category;

    KeyBinding(String constantName, String localization, String defaultKeyBinding, KeyBindingCategory category) {
        this.constant = constantName;
        this.localization = localization;
        this.defaultBinding = defaultKeyBinding;
        this.category = category;
    }

    /**
     * This method returns the enum constant value
     */
    public String getConstant() {
        return constant;
    }

    public String getLocalization() {
        return localization;
    }

    /**
     * This method returns the default key binding, the key(s) which are assigned
     *
     * @return The default key binding
     */
    public String getDefaultKeyBinding() {
        return defaultBinding;
    }

    public KeyBindingCategory getCategory() {
        return category;
    }
}
