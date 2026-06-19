package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Set;

import org.jabref.model.entry.field.Field;

/// Discriminated union of the kinds of tabs that appear in the entry editor.
///
/// {@link BuiltInTab}         — a fixed tab identified by a {@link BuiltIn} constant; only its visibility
///                             is configurable. Covers both the built-in field-set tabs (whose fields are
///                             computed dynamically from the entry type, see {@link BuiltIn#isFieldSet()})
///                             and the feature tabs (fixed implementations such as Preview or Source).
/// {@link CustomizedFieldSet} — user-customizable tab backed by an explicit, named set of fields.
public sealed interface EntryEditorTabModel
        permits EntryEditorTabModel.BuiltInTab, EntryEditorTabModel.CustomizedFieldSet {

    boolean isVisible();

    EntryEditorTabModel withVisible(boolean visible);

    default boolean isPreview() {
        return this instanceof BuiltInTab(
                BuiltIn type,
                boolean _
        ) && type == BuiltIn.PREVIEW;
    }

    static int indexAfterLeadingPreview(List<? extends EntryEditorTabModel> models) {
        return !models.isEmpty() && models.getFirst().isPreview() ? 1 : 0;
    }

    static int indexAfterBuiltInFieldSets(List<? extends EntryEditorTabModel> models) {
        int lastFieldSet = -1;
        for (int i = 0; i < models.size(); i++) {
            if (models.get(i) instanceof BuiltInTab(
                    BuiltIn type,
                    boolean _
            ) && type.isFieldSet()) {
                lastFieldSet = i;
            }
        }
        return lastFieldSet >= 0 ? lastFieldSet + 1 : indexAfterLeadingPreview(models);
    }

    /// Every fixed tab in the entry editor, in display order.
    enum BuiltIn {
        // Preview tab visibility controlled by preference option
        PREVIEW,

        // Built-in field-set tabs: fields computed dynamically from the entry type
        REQUIRED_FIELDS,
        IMPORTANT_OPTIONAL_FIELDS,
        DETAIL_OPTIONAL_FIELDS,
        DEPRECATED_FIELDS,
        OTHER_FIELDS,

        // Feature tabs: fixed implementations
        RELATED_ARTICLES,
        AI_SUMMARY,
        AI_CHAT,
        FILE_ANNOTATIONS,
        LATEX_CITATIONS,
        CITATION_INFORMATION,
        COMMENTS,
        MATH_SCI_NET,
        SOURCE,
        FULLTEXT_SEARCH_RESULTS;

        private static final Set<BuiltIn> FIELD_SETS = Set.of(
                REQUIRED_FIELDS, IMPORTANT_OPTIONAL_FIELDS, DETAIL_OPTIONAL_FIELDS, DEPRECATED_FIELDS, OTHER_FIELDS);

        public boolean isFieldSet() {
            return FIELD_SETS.contains(this);
        }
    }

    record BuiltInTab(BuiltIn type, boolean visible)
            implements EntryEditorTabModel {
        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public EntryEditorTabModel withVisible(boolean visible) {
            return new BuiltInTab(type, visible);
        }
    }

    /// Always shown; toggled only by being added to or removed from the tab list, so it carries no
    /// visibility flag (unlike {@link BuiltInTab}).
    record CustomizedFieldSet(String name, Set<Field> fields)
            implements EntryEditorTabModel {
        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public EntryEditorTabModel withVisible(boolean visible) {
            return this;
        }
    }
}
