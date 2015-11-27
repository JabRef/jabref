/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.keyboard;

import net.sf.jabref.gui.FindUnlinkedFilesDialog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KeyBinds {

    public static final String ABBREVIATE = "Abbreviate";
    public static final String AUTOGENERATE_BIB_TE_X_KEYS = "Autogenerate BibTeX keys";
    public static final String AUTOMATICALLY_LINK_FILES = "Automatically link files";
    public static final String BACK = "Back";
    public static final String BACK_HELP_DIALOG = "Back, help dialog";
    public static final String CLEANUP = "Cleanup";
    public static final String CLEAR_SEARCH = "Clear search";
    public static final String CLOSE_DATABASE = "Close database";
    public static final String CLOSE_DIALOG = "Close dialog";
    public static final String CLOSE_ENTRY_EDITOR = "Close entry editor";
    public static final String COPY = "Copy";
    public static final String COPY_BIB_TE_X_KEY = "Copy BibTeX key";
    public static final String COPY_BIB_TE_X_KEY_AND_TITLE = "Copy BibTeX key and title";
    public static final String COPY_CITE_BIB_TE_X_KEY = "Copy \\cite{BibTeX key}";
    public static final String CUT = "Cut";
    public static final String DECREASE_TABLE_FONT_SIZE = "Decrease table font size";
    public static final String DELETE_ENTRY = "Delete entry";
    public static final String EDIT_ENTRY = "Edit entry";
    public static final String EDIT_PREAMBLE = "Edit preamble";
    public static final String EDIT_STRINGS = "Edit strings";
    public static final String ENTRY_EDITOR_NEXT_ENTRY = "Entry editor, next entry";
    public static final String ENTRY_EDITOR_NEXT_PANEL = "Entry editor, next panel";
    public static final String ENTRY_EDITOR_NEXT_PANEL_2 = "Entry editor, next panel 2";
    public static final String ENTRY_EDITOR_PREVIOUS_ENTRY = "Entry editor, previous entry";
    public static final String ENTRY_EDITOR_PREVIOUS_PANEL = "Entry editor, previous panel";
    public static final String ENTRY_EDITOR_PREVIOUS_PANEL_2 = "Entry editor, previous panel 2";
    public static final String ENTRY_EDITOR_STORE_FIELD = "Entry editor, store field";
    public static final String FILE_LIST_EDITOR_MOVE_ENTRY_DOWN = "File list editor, move entry down";
    public static final String FILE_LIST_EDITOR_MOVE_ENTRY_UP = "File list editor, move entry up";
    public static final String FOCUS_ENTRY_TABLE = "Focus entry table";
    public static final String FORWARD = "Forward";
    public static final String FORWARD_HELP_DIALOG = "Forward, help dialog";
    public static final String HELP = "Help";
    public static final String HIDE_SHOW_TOOLBAR = "Hide/show toolbar";
    public static final String IMPORT_INTO_CURRENT_DATABASE = "Import into current database";
    public static final String IMPORT_INTO_NEW_DATABASE = "Import into new database";
    public static final String INCREASE_TABLE_FONT_SIZE = "Increase table font size";
    public static final String LOAD_SESSION = "Load session";
    public static final String MARK_ENTRIES = "Mark entries";
    public static final String NEW_ARTICLE = "New article";
    public static final String NEW_BOOK = "New book";
    public static final String NEW_ENTRY = "New entry";
    public static final String NEW_FILE_LINK = "New file link";
    public static final String NEW_FROM_PLAIN_TEXT = "New from plain text";
    public static final String NEW_INBOOK = "New inbook";
    public static final String NEW_MASTERSTHESIS = "New mastersthesis";
    public static final String NEW_PHDTHESIS = "New phdthesis";
    public static final String NEW_PROCEEDINGS = "New proceedings";
    public static final String NEW_UNPUBLISHED = "New unpublished";
    public static final String NEXT_TAB = "Next tab";
    public static final String OPEN_DATABASE = "Open database";
    public static final String OPEN_FILE = "Open file";
    public static final String OPEN_FOLDER = "Open folder";
    public static final String OPEN_SPIRES_ENTRY = "Open SPIRES entry";
    public static final String OPEN_URL_OR_DOI = "Open URL or DOI";
    public static final String PASTE = "Paste";
    public static final String PREAMBLE_EDITOR_STORE_CHANGES = "Preamble editor, store changes";
    public static final String PREVIOUS_TAB = "Previous tab";
    public static final String PRINT_ENTRY_PREVIEW = "Print entry preview";
    public static final String PUSH_TO_APPLICATION = "Push to application";
    public static final String QUIT_JAB_REF = "Quit JabRef";
    public static final String REDO = "Redo";
    public static final String REFRESH_OO = "Refresh OO";
    public static final String REPLACE_STRING = "Replace string";
    public static final String RESOLVE_DUPLICATE_BIB_TE_X_KEYS = "Resolve duplicate BibTeX keys";
    public static final String SAVE_ALL = "Save all";
    public static final String SAVE_DATABASE = "Save database";
    public static final String SAVE_DATABASE_AS = "Save database as ...";
    public static final String SAVE_SESSION = "Save session";
    public static final String SEARCH = "Search";
    public static final String SELECT_ALL = "Select all";
    public static final String STRING_DIALOG_ADD_STRING = "String dialog, add string";
    public static final String STRING_DIALOG_REMOVE_STRING = "String dialog, remove string";
    public static final String SWITCH_PREVIEW_LAYOUT = "Switch preview layout";
    public static final String SYNCHRONIZE_FILES = "Synchronize files";
    public static final String TOGGLE_ENTRY_PREVIEW = "Toggle entry preview";
    public static final String TOGGLE_GROUPS_INTERFACE = "Toggle groups interface";
    public static final String UNABBREVIATE = "Unabbreviate";
    public static final String UNDO = "Undo";
    public static final String UNMARK_ENTRIES = "Unmark entries";
    public static final String WEB_SEARCH = "Web search";
    public static final String WRITE_XMP = "Write XMP";

    private final HashMap<String, String> keyBindMap = new HashMap<>();


    public KeyBinds() {
        keyBindMap.put(PUSH_TO_APPLICATION, "ctrl L");
        keyBindMap.put(QUIT_JAB_REF, "ctrl Q");
        keyBindMap.put(OPEN_DATABASE, "ctrl O");
        keyBindMap.put(SAVE_DATABASE, "ctrl S");
        keyBindMap.put(SAVE_DATABASE_AS, "ctrl shift S");
        keyBindMap.put(SAVE_ALL, "ctrl alt S");
        keyBindMap.put(CLOSE_DATABASE, "ctrl W");
        keyBindMap.put(NEW_ENTRY, "ctrl N");
        keyBindMap.put(CUT, "ctrl X");
        keyBindMap.put(COPY, "ctrl C");
        keyBindMap.put(PASTE, "ctrl V");
        keyBindMap.put(UNDO, "ctrl Z");
        keyBindMap.put(REDO, "ctrl Y");
        keyBindMap.put(HELP, "F1");
        keyBindMap.put(NEW_ARTICLE, "ctrl shift A");
        keyBindMap.put(NEW_BOOK, "ctrl shift B");
        keyBindMap.put(NEW_PHDTHESIS, "ctrl shift T");
        keyBindMap.put(NEW_INBOOK, "ctrl shift I");
        keyBindMap.put(NEW_MASTERSTHESIS, "ctrl shift M");
        keyBindMap.put(NEW_PROCEEDINGS, "ctrl shift P");
        keyBindMap.put(NEW_UNPUBLISHED, "ctrl shift U");
        keyBindMap.put(EDIT_STRINGS, "ctrl T");
        keyBindMap.put(EDIT_PREAMBLE, "ctrl P");
        keyBindMap.put(SELECT_ALL, "ctrl A");
        keyBindMap.put(TOGGLE_GROUPS_INTERFACE, "ctrl shift G");
        keyBindMap.put(AUTOGENERATE_BIB_TE_X_KEYS, "ctrl G");
        keyBindMap.put(SEARCH, "ctrl F");
        keyBindMap.put(CLOSE_DIALOG, "ESCAPE");
        keyBindMap.put(BACK_HELP_DIALOG, "LEFT");
        keyBindMap.put(FORWARD_HELP_DIALOG, "RIGHT");
        keyBindMap.put(PREAMBLE_EDITOR_STORE_CHANGES, "alt S");
        keyBindMap.put(CLEAR_SEARCH, "ESCAPE");
        keyBindMap.put(CLOSE_ENTRY_EDITOR, "ESCAPE");
        keyBindMap.put(ENTRY_EDITOR_NEXT_PANEL, "ctrl TAB");//"ctrl PLUS");//"shift Right");
        keyBindMap.put(ENTRY_EDITOR_PREVIOUS_PANEL, "ctrl shift TAB");//"ctrl MINUS");
        keyBindMap.put(ENTRY_EDITOR_NEXT_PANEL_2, "ctrl PLUS");//"ctrl PLUS");//"shift Right");
        keyBindMap.put(ENTRY_EDITOR_PREVIOUS_PANEL_2, "ctrl MINUS");//"ctrl MINUS");
        keyBindMap.put(ENTRY_EDITOR_NEXT_ENTRY, "ctrl shift DOWN");
        keyBindMap.put(ENTRY_EDITOR_PREVIOUS_ENTRY, "ctrl shift UP");
        keyBindMap.put(ENTRY_EDITOR_STORE_FIELD, "alt S");
        keyBindMap.put(STRING_DIALOG_ADD_STRING, "ctrl N");
        keyBindMap.put(STRING_DIALOG_REMOVE_STRING, "shift DELETE");
        keyBindMap.put(SAVE_SESSION, "F11");
        keyBindMap.put(LOAD_SESSION, "F12");
        keyBindMap.put(COPY_CITE_BIB_TE_X_KEY, "ctrl K");
        keyBindMap.put(COPY_BIB_TE_X_KEY, "ctrl shift K");
        keyBindMap.put(COPY_BIB_TE_X_KEY_AND_TITLE, "ctrl shift alt K");
        keyBindMap.put(NEXT_TAB, "ctrl PAGE_DOWN");
        keyBindMap.put(PREVIOUS_TAB, "ctrl PAGE_UP");
        keyBindMap.put(REPLACE_STRING, "ctrl R");
        keyBindMap.put(DELETE_ENTRY, "DELETE");
        keyBindMap.put(OPEN_FILE, "F4");
        keyBindMap.put(OPEN_FOLDER, "ctrl shift O");
        keyBindMap.put(OPEN_URL_OR_DOI, "F3");
        keyBindMap.put(OPEN_SPIRES_ENTRY, "ctrl F3");
        keyBindMap.put(TOGGLE_ENTRY_PREVIEW, "ctrl F9");
        keyBindMap.put(SWITCH_PREVIEW_LAYOUT, "F9");
        keyBindMap.put(EDIT_ENTRY, "ctrl E");
        keyBindMap.put(MARK_ENTRIES, "ctrl M");
        keyBindMap.put(UNMARK_ENTRIES, "ctrl shift M");
        keyBindMap.put(WEB_SEARCH, "F5");
        keyBindMap.put(NEW_FROM_PLAIN_TEXT, "ctrl shift N");
        keyBindMap.put(SYNCHRONIZE_FILES, "ctrl F4");
        keyBindMap.put(FOCUS_ENTRY_TABLE, "ctrl shift E");
        keyBindMap.put(ABBREVIATE, "ctrl alt A");
        keyBindMap.put(UNABBREVIATE, "ctrl alt shift A");
        keyBindMap.put(CLEANUP, "ctrl shift F7");
        keyBindMap.put(WRITE_XMP, "ctrl F7");
        keyBindMap.put(NEW_FILE_LINK, "ctrl N");
        keyBindMap.put(BACK, "alt LEFT");
        keyBindMap.put(FORWARD, "alt RIGHT");
        keyBindMap.put(IMPORT_INTO_CURRENT_DATABASE, "ctrl I");
        keyBindMap.put(IMPORT_INTO_NEW_DATABASE, "ctrl alt I");
        keyBindMap.put(FindUnlinkedFilesDialog.ACTION_KEYBINDING_ACTION, "shift F7");
        keyBindMap.put(INCREASE_TABLE_FONT_SIZE, "ctrl PLUS");
        keyBindMap.put(DECREASE_TABLE_FONT_SIZE, "ctrl MINUS");
        keyBindMap.put(AUTOMATICALLY_LINK_FILES, "alt F");
        keyBindMap.put(RESOLVE_DUPLICATE_BIB_TE_X_KEYS, "ctrl shift D");
        keyBindMap.put(REFRESH_OO, "ctrl alt O");
        keyBindMap.put(FILE_LIST_EDITOR_MOVE_ENTRY_UP, "ctrl UP");
        keyBindMap.put(FILE_LIST_EDITOR_MOVE_ENTRY_DOWN, "ctrl DOWN");
        keyBindMap.put(HIDE_SHOW_TOOLBAR, "ctrl alt T");
        keyBindMap.put(PRINT_ENTRY_PREVIEW, "alt P");
    }

    public String get(String key) {
        return keyBindMap.get(key);
    }

    public HashMap<String, String> getKeyBindings() {
        return new HashMap<>(Collections.unmodifiableMap(keyBindMap));
    }

    public void overwriteBindings(Map<String, String> newBindings) {
        keyBindMap.clear();
        keyBindMap.putAll(newBindings);
    }

    public void put(String key, String value) {
        keyBindMap.put(key, value);
    }

}
