package org.jabref.gui.entryeditor;

import java.util.Set;

import org.jabref.model.entry.field.Field;

/// Discriminated union of the kinds of tabs that appear in the entry editor.
///
/// {@link FieldSet}          — built-in tab (e.g. {@link RequiredFieldsTab}) whose fields are computed
///                             dynamically from the entry type; only its visibility is configurable.
/// {@link CustomizedFieldSet} — user-customizable tab backed by an explicit, named set of fields.
/// {@link Feature}            — fixed-implementation tab whose only user-facing knob is visibility.
public sealed interface EntryEditorTabModel
        permits EntryEditorTabModel.FieldSet, EntryEditorTabModel.CustomizedFieldSet, EntryEditorTabModel.Feature {

    boolean visible();

    enum StaticTab {
        RELATED_ARTICLES,
        AI_SUMMARY,
        AI_CHAT,
        FILE_ANNOTATIONS,
        LATEX_CITATIONS,
        CITATION_INFORMATION,
        USER_COMMENTS
    }

    enum BuiltInFieldSet {
        REQUIRED_FIELDS,
        IMPORTANT_OPTIONAL_FIELDS,
        DETAIL_OPTIONAL_FIELDS,
        DEPRECATED_FIELDS,
        OTHER_FIELDS
    }

    record FieldSet(BuiltInFieldSet type, boolean visible)
            implements EntryEditorTabModel {
    }

    record CustomizedFieldSet(String name, Set<Field> fields, boolean visible)
            implements EntryEditorTabModel {
    }

    record Feature(StaticTab type, boolean visible)
            implements EntryEditorTabModel {
    }
}
