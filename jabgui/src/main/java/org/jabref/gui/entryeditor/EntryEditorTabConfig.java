package org.jabref.gui.entryeditor;

import java.util.Set;

import org.jabref.model.entry.field.Field;

/// Discriminated union of the two kinds of tabs that appear in the entry editor.
///
/// {@link FieldSet} — user-customizable tab backed by a named set of fields.
/// {@link Feature}  — fixed-implementation tab whose only user-facing knob is visibility.
public sealed interface EntryEditorTabConfig
        permits EntryEditorTabConfig.FieldSet, EntryEditorTabConfig.Feature {

    boolean visible();

    record FieldSet(String name, Set<Field> fields, boolean visible)
            implements EntryEditorTabConfig {
    }

    record Feature(EntryEditorPreferences.StaticTab type, boolean visible)
            implements EntryEditorTabConfig {
    }
}
