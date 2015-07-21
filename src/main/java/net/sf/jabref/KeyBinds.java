package net.sf.jabref;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KeyBinds {

    private final HashMap<String, String> keyBindMap = new HashMap<String, String>();

    public KeyBinds() {
        keyBindMap.put("Push to application", "ctrl L");
        keyBindMap.put("Push to LyX", "ctrl L");
        keyBindMap.put("Push to WinEdt", "ctrl shift W");
        keyBindMap.put("Quit JabRef", "ctrl Q");
        keyBindMap.put("Open database", "ctrl O");
        keyBindMap.put("Save database", "ctrl S");
        keyBindMap.put("Save database as ...", "ctrl shift S");
        keyBindMap.put("Save all", "ctrl alt S");
        keyBindMap.put("Close database", "ctrl W");
        keyBindMap.put("New entry", "ctrl N");
        keyBindMap.put("Cut", "ctrl X");
        keyBindMap.put("Copy", "ctrl C");
        keyBindMap.put("Paste", "ctrl V");
        keyBindMap.put("Undo", "ctrl Z");
        keyBindMap.put("Redo", "ctrl Y");
        keyBindMap.put("Help", "F1");
        keyBindMap.put("New article", "ctrl shift A");
        keyBindMap.put("New book", "ctrl shift B");
        keyBindMap.put("New phdthesis", "ctrl shift T");
        keyBindMap.put("New inbook", "ctrl shift I");
        keyBindMap.put("New mastersthesis", "ctrl shift M");
        keyBindMap.put("New proceedings", "ctrl shift P");
        keyBindMap.put("New unpublished", "ctrl shift U");
        keyBindMap.put("Edit strings", "ctrl T");
        keyBindMap.put("Edit preamble", "ctrl P");
        keyBindMap.put("Select all", "ctrl A");
        keyBindMap.put("Toggle groups interface", "ctrl shift G");
        keyBindMap.put("Autogenerate BibTeX keys", "ctrl G");
        keyBindMap.put("Search", "ctrl F");
        keyBindMap.put("Incremental search", "ctrl shift F");
        keyBindMap.put("Repeat incremental search", "ctrl shift F");
        keyBindMap.put("Close dialog", "ESCAPE");
        keyBindMap.put("Close entry editor", "ESCAPE");
        keyBindMap.put("Close preamble editor", "ESCAPE");
        keyBindMap.put("Back, help dialog", "LEFT");
        keyBindMap.put("Forward, help dialog", "RIGHT");
        keyBindMap.put("Preamble editor, store changes", "alt S");
        keyBindMap.put("Clear search", "ESCAPE");
        keyBindMap.put("Entry editor, next panel", "ctrl TAB");//"ctrl PLUS");//"shift Right");
        keyBindMap.put("Entry editor, previous panel", "ctrl shift TAB");//"ctrl MINUS");
        keyBindMap.put("Entry editor, next panel 2", "ctrl PLUS");//"ctrl PLUS");//"shift Right");
        keyBindMap.put("Entry editor, previous panel 2", "ctrl MINUS");//"ctrl MINUS");
        keyBindMap.put("Entry editor, next entry", "ctrl shift DOWN");
        keyBindMap.put("Entry editor, previous entry", "ctrl shift UP");
        keyBindMap.put("Entry editor, store field", "alt S");
        keyBindMap.put("String dialog, add string", "ctrl N");
        keyBindMap.put("String dialog, remove string", "shift DELETE");
        keyBindMap.put("String dialog, move string up", "ctrl UP");
        keyBindMap.put("String dialog, move string down", "ctrl DOWN");
        keyBindMap.put("Save session", "F11");
        keyBindMap.put("Load session", "F12");
        keyBindMap.put("Copy \\cite{BibTeX key}", "ctrl K");
        keyBindMap.put("Copy BibTeX key", "ctrl shift K");
        keyBindMap.put("Copy BibTeX key and title", "ctrl shift alt K");
        keyBindMap.put("Next tab", "ctrl PAGE_DOWN");
        keyBindMap.put("Previous tab", "ctrl PAGE_UP");
        keyBindMap.put("Replace string", "ctrl R");
        keyBindMap.put("Delete", "DELETE");
        keyBindMap.put("Open file", "F4");
        keyBindMap.put("Open folder", "ctrl shift O");
        keyBindMap.put("Open PDF or PS", "shift F5");
        keyBindMap.put("Open URL or DOI", "F3");
        keyBindMap.put("Open SPIRES entry", "ctrl F3");
        keyBindMap.put("Toggle entry preview", "ctrl F9");
        keyBindMap.put("Switch preview layout", "F9");
        keyBindMap.put("Edit entry", "ctrl E");
        keyBindMap.put("Mark entries", "ctrl M");
        keyBindMap.put("Unmark entries", "ctrl shift M");
        keyBindMap.put("Fetch Medline", "F5");
        keyBindMap.put("Search ScienceDirect", "ctrl F5");
        keyBindMap.put("Search ADS", "ctrl shift F6");
        keyBindMap.put("New from plain text", "ctrl shift N");
        keyBindMap.put("Synchronize files", "ctrl F4");
        keyBindMap.put("Synchronize PDF", "shift F4");
        keyBindMap.put("Synchronize PS", "ctrl shift F4");
        keyBindMap.put("Focus entry table", "ctrl shift E");

        keyBindMap.put("Abbreviate", "ctrl alt A");
        keyBindMap.put("Unabbreviate", "ctrl alt shift A");
        keyBindMap.put("Search IEEEXplore", "alt F8");
        keyBindMap.put("Search ACM Portal", "ctrl shift F8");
        keyBindMap.put("Fetch ArXiv.org", "shift F8");
        keyBindMap.put("Search JSTOR", "shift F9");
        keyBindMap.put("Cleanup", "ctrl shift F7");
        keyBindMap.put("Write XMP", "ctrl F7");
        keyBindMap.put("New file link", "ctrl N");
        keyBindMap.put("Fetch SPIRES", "ctrl F8");
        keyBindMap.put("Fetch INSPIRE", "ctrl F2");
        keyBindMap.put("Back", "alt LEFT");
        keyBindMap.put("Forward", "alt RIGHT");
        keyBindMap.put("Import into current database", "ctrl I");
        keyBindMap.put("Import into new database", "ctrl alt I");
        keyBindMap.put(FindUnlinkedFilesDialog.ACTION_KEYBINDING_ACTION, "shift F7");
        keyBindMap.put("Increase table font size", "ctrl PLUS");
        keyBindMap.put("Decrease table font size", "ctrl MINUS");
        keyBindMap.put("Automatically link files", "alt F");
        keyBindMap.put("Resolve duplicate BibTeX keys", "ctrl shift D");
        keyBindMap.put("Refresh OO", "ctrl alt O");
        keyBindMap.put("File list editor, move entry up", "ctrl UP");
        keyBindMap.put("File list editor, move entry down", "ctrl DOWN");
        keyBindMap.put("Minimize to system tray", "ctrl alt W");
        keyBindMap.put("Hide/show toolbar", "ctrl alt T");
    }

    public String get(String key) {
        return keyBindMap.get(key);
    }

    public void put(String key, String value) {
        keyBindMap.put(key, value);
    }

    public HashMap<String, String> getKeyBindings() {
        return new HashMap<String, String>(Collections.unmodifiableMap(keyBindMap));
    }

    public void overwriteBindings(Map<String, String> newBindings) {
        keyBindMap.clear();
        keyBindMap.putAll(newBindings);
    }

}
