package net.sf.jabref.gui.keyboard;

import net.sf.jabref.logic.l10n.Localization;

public enum KeyBinding {

    ABBREVIATE(
            "Abbreviate", Localization.lang("Abbreviate journal names"), "ctrl alt A", KeyBindingCategory.TOOLS),
    AUTOGENERATE_BIBTEX_KEYS(
            "Autogenerate BibTeX keys", Localization
                    .lang("Autogenerate BibTeX keys"), "ctrl G", KeyBindingCategory.QUALITY),
    AUTOMATICALLY_LINK_FILES(
            "Automatically link files", Localization.lang("Automatically set file links"), "alt F", KeyBindingCategory.QUALITY),
    BACK("Back", Localization.lang("Back"), "alt LEFT", KeyBindingCategory.VIEW),
    CLEANUP(
            "Cleanup", Localization.lang("Cleanup entries"), "ctrl shift F7", KeyBindingCategory.QUALITY),
    CLEAR_SEARCH("Clear search", Localization.lang("Clear search"), "ESCAPE", KeyBindingCategory.SEARCH),
    CLOSE_DATABASE("Close database", Localization.lang("Close database"), "ctrl W", KeyBindingCategory.FILE),
    CLOSE_DIALOG("Close dialog", Localization.lang("Close dialog"), "ESCAPE", KeyBindingCategory.FILE),
    CLOSE_ENTRY_EDITOR(
            "Close entry editor", Localization.lang("Close entry editor"), "ESCAPE", KeyBindingCategory.VIEW),
    COPY("Copy", Localization.lang("Copy"), "ctrl C", KeyBindingCategory.EDIT),
    COPY_CITE_BIBTEX_KEY("Copy \\cite{BibTeX key}", Localization.lang("Copy \\cite{BibTeX key}"), "ctrl K", KeyBindingCategory.EDIT),
    COPY_BIBTEX_KEY("Copy BibTeX key", Localization.lang("Copy BibTeX key"), "ctrl shift K", KeyBindingCategory.EDIT),
    COPY_BIBTEX_KEY_AND_TITLE("Copy BibTeX key and title", Localization.lang("Copy BibTeX key and title"), "ctrl shift alt K", KeyBindingCategory.EDIT),
    COPY_PREVIEW(
            "Copy preview", Localization.lang("Copy preview"), "ctrl shift C", KeyBindingCategory.VIEW),
    CUT("Cut", Localization.lang("Cut"), "ctrl X", KeyBindingCategory.EDIT),
    DECREASE_TABLE_FONT_SIZE("Decrease table font size", Localization.lang("Decrease table font size"), "ctrl MINUS", KeyBindingCategory.VIEW),
    DELETE_ENTRY("Delete entry", Localization.lang("Delete entry"), "DELETE", KeyBindingCategory.BIBTEX),
    EDIT_ENTRY("Edit entry", Localization.lang("Edit entry"), "ctrl E", KeyBindingCategory.BIBTEX),
    EDIT_PREAMBLE("Edit preamble", Localization.lang("Edit preamble"), "ctrl P", KeyBindingCategory.BIBTEX),
    EDIT_STRINGS("Edit strings", Localization.lang("Edit strings"), "ctrl T", KeyBindingCategory.BIBTEX),
    ENTRY_EDITOR_NEXT_ENTRY("Entry editor, next entry", Localization.lang("Entry editor, next entry"), "ctrl shift DOWN", KeyBindingCategory.VIEW),
    ENTRY_EDITOR_NEXT_PANEL("Entry editor, next panel", Localization.lang("Entry editor, next panel"), "ctrl TAB", KeyBindingCategory.VIEW),
    ENTRY_EDITOR_NEXT_PANEL_2("Entry editor, next panel 2", Localization.lang("Entry editor, next panel 2"), "ctrl PLUS", KeyBindingCategory.VIEW),
    ENTRY_EDITOR_PREVIOUS_ENTRY("Entry editor, previous entry", Localization.lang("Entry editor, previous entry"), "ctrl shift UP", KeyBindingCategory.VIEW),
    ENTRY_EDITOR_PREVIOUS_PANEL("Entry editor, previous panel", Localization.lang("Entry editor, previous panel"), "ctrl shift TAB", KeyBindingCategory.VIEW),
    ENTRY_EDITOR_PREVIOUS_PANEL_2("Entry editor, previous panel 2", Localization.lang("Entry editor, previous panel 2"), "ctrl MINUS", KeyBindingCategory.VIEW),
    ENTRY_EDITOR_STORE_FIELD("Entry editor, store field", Localization.lang("Entry editor, store field"), "alt S", KeyBindingCategory.TOOLS),
    FILE_LIST_EDITOR_MOVE_ENTRY_DOWN("File list editor, move entry down", Localization.lang("File list editor, move entry down"), "ctrl DOWN", KeyBindingCategory.VIEW),
    FILE_LIST_EDITOR_MOVE_ENTRY_UP("File list editor, move entry up", Localization.lang("File list editor, move entry up"), "ctrl UP", KeyBindingCategory.VIEW),
    FIND_UNLINKED_FILES(
            "Find unlinked files", Localization.lang("Find unlinked files"), "shift F7", KeyBindingCategory.QUALITY),
    FOCUS_ENTRY_TABLE("Focus entry table", Localization.lang("Focus entry table"), "ctrl shift E", KeyBindingCategory.VIEW),
    FORWARD("Forward", Localization.lang("Forward"), "alt RIGHT", KeyBindingCategory.VIEW),
    HELP("Help", Localization.lang("Help"), "F1", KeyBindingCategory.FILE),
    HIDE_SHOW_TOOLBAR(
            "Hide/show toolbar", Localization.lang("Hide/show toolbar"), "ctrl alt T", KeyBindingCategory.VIEW),
    IMPORT_INTO_CURRENT_DATABASE("Import into current database", Localization.lang("Import into current database"), "ctrl I", KeyBindingCategory.FILE),
    IMPORT_INTO_NEW_DATABASE("Import into new database", Localization.lang("Import into new database"), "ctrl alt I", KeyBindingCategory.FILE),
    INCREASE_TABLE_FONT_SIZE(
            "Increase table font size", Localization
                    .lang("Increase table font size"), "ctrl PLUS", KeyBindingCategory.VIEW),
    MARK_ENTRIES("Mark entries", Localization.lang("Mark entries"), "ctrl M", KeyBindingCategory.EDIT),
    NEW_ARTICLE("New article", Localization.lang("New article"), "ctrl shift A", KeyBindingCategory.BIBTEX),
    NEW_BOOK("New book", Localization.lang("New book"), "ctrl shift B", KeyBindingCategory.BIBTEX),
    NEW_ENTRY("New entry", Localization.lang("New entry"), "ctrl N", KeyBindingCategory.BIBTEX),
    NEW_FROM_PLAIN_TEXT("New from plain text", Localization.lang("New from plain text"), "ctrl shift N", KeyBindingCategory.BIBTEX),
    NEW_INBOOK("New inbook", Localization.lang("New inbook"), "ctrl shift I", KeyBindingCategory.BIBTEX),
    NEW_MASTERSTHESIS("New mastersthesis", Localization.lang("New mastersthesis"), "ctrl shift M", KeyBindingCategory.BIBTEX),
    NEW_PHDTHESIS("New phdthesis", Localization.lang("New phdthesis"), "ctrl shift T", KeyBindingCategory.BIBTEX),
    NEW_PROCEEDINGS("New proceedings", Localization.lang("New proceedings"), "ctrl shift P", KeyBindingCategory.BIBTEX),
    NEW_UNPUBLISHED("New unpublished", Localization.lang("New unpublished"), "ctrl shift U", KeyBindingCategory.BIBTEX),
    NEXT_TAB(
            "Next tab", Localization.lang("Next tab"), "ctrl PAGE_DOWN", KeyBindingCategory.VIEW),
    OPEN_CONSOLE("Open terminal here", Localization.lang("Open terminal here"), "ctrl shift J", KeyBindingCategory.TOOLS),
    OPEN_DATABASE("Open database", Localization.lang("Open database"), "ctrl O", KeyBindingCategory.FILE),
    OPEN_FILE("Open file", Localization.lang("Open file"), "F4", KeyBindingCategory.TOOLS),
    OPEN_FOLDER("Open folder", Localization.lang("Open folder"), "ctrl shift O", KeyBindingCategory.TOOLS),
    OPEN_URL_OR_DOI("Open URL or DOI", Localization.lang("Open URL or DOI"), "F3", KeyBindingCategory.TOOLS),
    PASTE("Paste", Localization.lang("Paste"), "ctrl V", KeyBindingCategory.EDIT),
    PREAMBLE_EDITOR_STORE_CHANGES("Preamble editor, store changes", Localization.lang("Preamble editor, store changes"), "alt S", KeyBindingCategory.FILE),
    PREVIOUS_TAB(
            "Previous tab", Localization.lang("Previous tab"), "ctrl PAGE_UP", KeyBindingCategory.VIEW),
    PRINT_ENTRY_PREVIEW("Print entry preview", Localization.lang("Print entry preview"), "alt P", KeyBindingCategory.FILE),
    PUSH_TO_APPLICATION("Push to application", Localization.lang("Push to application"), "ctrl L", KeyBindingCategory.TOOLS),
    QUIT_JABREF("Quit JabRef", Localization.lang("Quit JabRef"), "ctrl Q", KeyBindingCategory.FILE),
    REDO("Redo", Localization.lang("Redo"), "ctrl Y", KeyBindingCategory.EDIT),
    REFRESH_OO(
            "Refresh OO", Localization.lang("Refresh OpenOffice/LibreOffice"), "ctrl alt O", KeyBindingCategory.TOOLS),
    REPLACE_STRING("Replace string", Localization.lang("Replace string"), "ctrl R", KeyBindingCategory.SEARCH),
    RESOLVE_DUPLICATE_BIBTEX_KEYS("Resolve duplicate BibTeX keys", Localization.lang("Resolve duplicate BibTeX keys"), "ctrl shift D", KeyBindingCategory.BIBTEX),
    SAVE_ALL("Save all", Localization.lang("Save all"), "ctrl alt S", KeyBindingCategory.FILE),
    SAVE_DATABASE("Save database", Localization.lang("Save database"), "ctrl S", KeyBindingCategory.FILE),
    SAVE_DATABASE_AS(
            "Save database as ...", Localization.lang("Save database as..."), "ctrl shift S", KeyBindingCategory.FILE),
    SEARCH("Search", Localization.lang("Search"), "ctrl F", KeyBindingCategory.SEARCH),
    SELECT_ALL("Select all", Localization.lang("Select all"), "ctrl A", KeyBindingCategory.EDIT),
    STRING_DIALOG_ADD_STRING("String dialog, add string", Localization.lang("String dialog, add string"), "ctrl N", KeyBindingCategory.FILE),
    STRING_DIALOG_REMOVE_STRING("String dialog, remove string", Localization.lang("String dialog, remove string"), "shift DELETE", KeyBindingCategory.FILE),
    SWITCH_PREVIEW_LAYOUT(
            "Switch preview layout", Localization.lang("Switch preview layout"), "F9", KeyBindingCategory.VIEW),
    SYNCHRONIZE_FILES("Synchronize files", Localization.lang("Synchronize files"), "ctrl F4", KeyBindingCategory.QUALITY),
    TOGGLE_ENTRY_PREVIEW(
            "Toggle entry preview", Localization.lang("Toggle entry preview"), "ctrl F9", KeyBindingCategory.VIEW),
    TOGGLE_GROUPS_INTERFACE("Toggle groups interface", Localization.lang("Toggle groups interface"), "ctrl shift G", KeyBindingCategory.VIEW),
    UNABBREVIATE("Unabbreviate", Localization.lang("Unabbreviate"), "ctrl alt shift A", KeyBindingCategory.TOOLS),
    UNDO("Undo", Localization.lang("Undo"), "ctrl Z", KeyBindingCategory.EDIT),
    UNMARK_ENTRIES("Unmark entries", Localization.lang("Unmark entries"), "ctrl shift M", KeyBindingCategory.EDIT),
    WEB_SEARCH("Web search", Localization.lang("Web search"), "F5", KeyBindingCategory.SEARCH),
    WRITE_XMP("Write XMP", Localization.lang("Write XMP"), "ctrl F7", KeyBindingCategory.TOOLS),;

    private final String key;
    private final String localization;
    private final String defaultBinding;
    private final KeyBindingCategory category;


    KeyBinding(String key, String localization, String defaultBinding, KeyBindingCategory category) {
        this.key = key;
        this.localization = localization;
        this.defaultBinding = defaultBinding;
        this.category = category;
    }

    public String getKey() {
        return key;
    }

    public String getLocalization() {
        return localization;
    }

    public String getDefaultBinding() {
        return defaultBinding;
    }

    public KeyBindingCategory getCategory() {
        return category;
    }

}
