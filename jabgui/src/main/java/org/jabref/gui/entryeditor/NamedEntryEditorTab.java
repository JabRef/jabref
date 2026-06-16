package org.jabref.gui.entryeditor;

/// Marks a built-in {@link EntryEditorTab} that has a fixed, well-known name (its preferences/config key).
///
/// {@link EntryEditorTabFactory} uses this to automatically exclude built-in tabs from the
/// user-configured tab list, instead of maintaining a separate hard-coded exclusion list.
public interface NamedEntryEditorTab {
    String getName();
}
