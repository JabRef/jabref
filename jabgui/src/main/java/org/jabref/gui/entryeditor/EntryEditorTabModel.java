package org.jabref.gui.entryeditor;

import java.util.Set;

import org.jabref.model.entry.field.Field;

/// Discriminated union of the two kinds of tabs that appear in the entry editor.
///
/// {@link FieldSet} — user-customizable tab backed by a named set of fields.
/// {@link Feature}  — fixed-implementation tab whose only user-facing knob is visibility.
public sealed interface EntryEditorTabModel
        permits EntryEditorTabModel.FieldSet, EntryEditorTabModel.Feature {

    boolean visible();

    enum StaticTab {
        REQUIRED_FIELDS,
        IMPORTANT_OPTIONAL_FIELDS,
        DETAIL_OPTIONAL_FIELDS,
        DEPRECATED_FIELDS,
        OTHER_FIELDS,
        RELATED_ARTICLES,
        AI_SUMMARY,
        AI_CHAT,
        FILE_ANNOTATIONS,
        LATEX_CITATIONS,
        CITATION_INFORMATION,
        USER_COMMENTS
    }

    record FieldSet(String name, Set<Field> fields, boolean visible)
            implements EntryEditorTabModel {
    }

    record Feature(StaticTab type, boolean visible)
            implements EntryEditorTabModel {
    }
}
