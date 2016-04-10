package net.sf.jabref.gui.keyboard;

import net.sf.jabref.logic.l10n.Localization;

public enum KeyBinding {

    ABBREVIATE(
            "Abbreviate", Localization.lang("Abbreviate journal names"), "ctrl alt A"),
    AUTOGENERATE_BIBTEX_KEYS("Autogenerate BibTeX keys", Localization.lang("Autogenerate BibTeX keys"), "ctrl G"),
    AUTOMATICALLY_LINK_FILES(
            "Automatically link files", Localization.lang("Automatically set file links"), "alt F"),
    BACK("Back", Localization.lang("Back"), "alt LEFT"),
    CLEANUP(
            "Cleanup", Localization.lang("Cleanup entries"), "ctrl shift F7"),
    CLEAR_SEARCH("Clear search", Localization.lang("Clear search"), "ESCAPE"),
    CLOSE_DATABASE("Close database", Localization.lang("Close database"), "ctrl W"),
    CLOSE_DIALOG("Close dialog", Localization.lang("Close dialog"), "ESCAPE"),
    CLOSE_ENTRY_EDITOR("Close entry editor", Localization.lang("Close entry editor"), "ESCAPE"),
    COPY("Copy", Localization.lang("Copy"), "ctrl C"),
    COPY_CITE_BIBTEX_KEY("Copy \\cite{BibTeX key}", Localization.lang("Copy \\cite{BibTeX key}"), "ctrl K"),
    COPY_BIBTEX_KEY("Copy BibTeX key", Localization.lang("Copy BibTeX key"), "ctrl shift K"),
    COPY_BIBTEX_KEY_AND_TITLE("Copy BibTeX key and title", Localization.lang("Copy BibTeX key and title"), "ctrl shift alt K"),
    COPY_PREVIEW("Copy preview", Localization.lang("Copy preview"), "ctrl shift C"),
    CUT("Cut", Localization.lang("Cut"), "ctrl X"),
    DECREASE_TABLE_FONT_SIZE("Decrease table font size", Localization.lang("Decrease table font size"), "ctrl MINUS"),
    DELETE_ENTRY("Delete entry", Localization.lang("Delete entry"), "DELETE"),
    EDIT_ENTRY("Edit entry", Localization.lang("Edit entry"), "ctrl E"),
    EDIT_PREAMBLE("Edit preamble", Localization.lang("Edit preamble"), "ctrl P"),
    EDIT_STRINGS("Edit strings", Localization.lang("Edit strings"), "ctrl T"),
    ENTRY_EDITOR_NEXT_ENTRY("Entry editor, next entry", Localization.lang("Entry editor, next entry"), "ctrl shift DOWN"),
    ENTRY_EDITOR_NEXT_PANEL("Entry editor, next panel", Localization.lang("Entry editor, next panel"), "ctrl TAB"),
    ENTRY_EDITOR_NEXT_PANEL_2("Entry editor, next panel 2", Localization.lang("Entry editor, next panel 2"), "ctrl PLUS"),
    ENTRY_EDITOR_PREVIOUS_ENTRY("Entry editor, previous entry", Localization.lang("Entry editor, previous entry"), "ctrl shift UP"),
    ENTRY_EDITOR_PREVIOUS_PANEL("Entry editor, previous panel", Localization.lang("Entry editor, previous panel"), "ctrl shift TAB"),
    ENTRY_EDITOR_PREVIOUS_PANEL_2("Entry editor, previous panel 2", Localization.lang("Entry editor, previous panel 2"), "ctrl MINUS"),
    ENTRY_EDITOR_STORE_FIELD("Entry editor, store field", Localization.lang("Entry editor, store field"), "alt S"),
    FILE_LIST_EDITOR_MOVE_ENTRY_DOWN("File list editor, move entry down", Localization.lang("File list editor, move entry down"), "ctrl DOWN"),
    FILE_LIST_EDITOR_MOVE_ENTRY_UP("File list editor, move entry up", Localization.lang("File list editor, move entry up"), "ctrl UP"),
    FIND_UNLINKED_FILES("Find unlinked files", Localization.lang("Find unlinked files"), "shift F7"),
    FOCUS_ENTRY_TABLE("Focus entry table", Localization.lang("Focus entry table"), "ctrl shift E"),
    FORWARD("Forward", Localization.lang("Forward"), "alt RIGHT"),
    HELP("Help", Localization.lang("Help"), "F1"),
    HIDE_SHOW_TOOLBAR("Hide/show toolbar", Localization.lang("Hide/show toolbar"), "ctrl alt T"),
    IMPORT_INTO_CURRENT_DATABASE("Import into current database", Localization.lang("Import into current database"), "ctrl I"),
    IMPORT_INTO_NEW_DATABASE("Import into new database", Localization.lang("Import into new database"), "ctrl alt I"),
    INCREASE_TABLE_FONT_SIZE("Increase table font size", Localization.lang("Increase table font size"), "ctrl PLUS"),
    MARK_ENTRIES("Mark entries", Localization.lang("Mark entries"), "ctrl M"),
    NEW_ARTICLE("New article", Localization.lang("New article"), "ctrl shift A"),
    NEW_BOOK("New book", Localization.lang("New book"), "ctrl shift B"),
    NEW_ENTRY("New entry", Localization.lang("New entry"), "ctrl N"),
    NEW_FROM_PLAIN_TEXT("New from plain text", Localization.lang("New from plain text"), "ctrl shift N"),
    NEW_INBOOK("New inbook", Localization.lang("New inbook"), "ctrl shift I"),
    NEW_MASTERSTHESIS("New mastersthesis", Localization.lang("New mastersthesis"), "ctrl shift M"),
    NEW_PHDTHESIS("New phdthesis", Localization.lang("New phdthesis"), "ctrl shift T"),
    NEW_PROCEEDINGS("New proceedings", Localization.lang("New proceedings"), "ctrl shift P"),
    NEW_UNPUBLISHED("New unpublished", Localization.lang("New unpublished"), "ctrl shift U"),
    NEXT_TAB("Next tab", Localization.lang("Next tab"), "ctrl PAGE_DOWN"),
    OPEN_CONSOLE("Open terminal here", Localization.lang("Open terminal here"), "ctrl shift J"),
    OPEN_DATABASE("Open database", Localization.lang("Open database"), "ctrl O"),
    OPEN_FILE("Open file", Localization.lang("Open file"), "F4"),
    OPEN_FOLDER("Open folder", Localization.lang("Open folder"), "ctrl shift O"),
    OPEN_URL_OR_DOI("Open URL or DOI", Localization.lang("Open URL or DOI"), "F3"),
    PASTE("Paste", Localization.lang("Paste"), "ctrl V"),
    PREAMBLE_EDITOR_STORE_CHANGES("Preamble editor, store changes", Localization.lang("Preamble editor, store changes"), "alt S"),
    PREVIOUS_TAB("Previous tab", Localization.lang("Previous tab"), "ctrl PAGE_UP"),
    PRINT_ENTRY_PREVIEW("Print entry preview", Localization.lang("Print entry preview"), "alt P"),
    PUSH_TO_APPLICATION("Push to application", Localization.lang("Push to application"), "ctrl L"),
    QUIT_JABREF("Quit JabRef", Localization.lang("Quit JabRef"), "ctrl Q"),
    REDO("Redo", Localization.lang("Redo"), "ctrl Y"),
    REFRESH_OO(
            "Refresh OO", Localization.lang("Refresh OpenOffice/LibreOffice"), "ctrl alt O"),
    REPLACE_STRING("Replace string", Localization.lang("Replace string"), "ctrl R"),
    RESOLVE_DUPLICATE_BIBTEX_KEYS("Resolve duplicate BibTeX keys", Localization.lang("Resolve duplicate BibTeX keys"), "ctrl shift D"),
    SAVE_ALL("Save all", Localization.lang("Save all"), "ctrl alt S"),
    SAVE_DATABASE("Save database", Localization.lang("Save database"), "ctrl S"),
    SAVE_DATABASE_AS(
            "Save database as ...", Localization.lang("Save database as..."), "ctrl shift S"),
    SEARCH("Search", Localization.lang("Search"), "ctrl F"),
    SELECT_ALL("Select all", Localization.lang("Select all"), "ctrl A"),
    STRING_DIALOG_ADD_STRING("String dialog, add string", Localization.lang("String dialog, add string"), "ctrl N"),
    STRING_DIALOG_REMOVE_STRING("String dialog, remove string", Localization.lang("String dialog, remove string"), "shift DELETE"),
    SWITCH_PREVIEW_LAYOUT("Switch preview layout", Localization.lang("Switch preview layout"), "F9"),
    SYNCHRONIZE_FILES("Synchronize files", Localization.lang("Synchronize files"), "ctrl F4"),
    TOGGLE_ENTRY_PREVIEW("Toggle entry preview", Localization.lang("Toggle entry preview"), "ctrl F9"),
    TOGGLE_GROUPS_INTERFACE("Toggle groups interface", Localization.lang("Toggle groups interface"), "ctrl shift G"),
    UNABBREVIATE("Unabbreviate", Localization.lang("Unabbreviate"), "ctrl alt shift A"),
    UNDO("Undo", Localization.lang("Undo"), "ctrl Z"),
    UNMARK_ENTRIES("Unmark entries", Localization.lang("Unmark entries"), "ctrl shift M"),
    WEB_SEARCH("Web search", Localization.lang("Web search"), "F5"),
    WRITE_XMP("Write XMP", Localization.lang("Write XMP"), "ctrl F7"),;

    private final String key;
    private final String localization;
    private final String defaultBinding;

    KeyBinding(String key, String localization, String defaultBinding) {
        this.key = key;
        this.localization = localization;
        this.defaultBinding = defaultBinding;
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

}
