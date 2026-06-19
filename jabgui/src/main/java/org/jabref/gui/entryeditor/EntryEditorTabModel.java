package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Optional;
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

    /// This tab's built-in identity, or empty for a {@link CustomizedFieldSet} (always visible, no toggle).
    Optional<BuiltIn> key();

    /// Whether this tab is currently shown. {@link CustomizedFieldSet} is always visible.
    boolean isVisible();

    /// A copy of this tab with the given visibility. No-op for {@link CustomizedFieldSet}.
    EntryEditorTabModel withVisible(boolean visible);

    /// Whether this is the always-present leading Preview tab (its visibility is owned by
    /// PreviewPreferences; customized field-set tabs are inserted right after it).
    default boolean isPreview() {
        return key().filter(type -> type == BuiltIn.PREVIEW).isPresent();
    }

    /// Index just past a leading {@link BuiltIn#PREVIEW} tab in {@code models} (1 if present, else 0),
    /// so customized field-set tabs are inserted after the Preview tab instead of before it.
    static int indexAfterLeadingPreview(List<? extends EntryEditorTabModel> models) {
        return !models.isEmpty() && models.getFirst().isPreview() ? 1 : 0;
    }

    /// The one canonical insertion point for customized field-set tabs: right after the built-in
    /// {@linkplain BuiltIn#isFieldSet() field-set} tabs (so they sit between the built-in field sets and the
    /// feature tabs). This must match the persisted load order; otherwise saving silently reorders the tabs
    /// on every round-trip. Falls back to just after a leading Preview tab when no built-in field sets are present.
    static int indexAfterBuiltInFieldSets(List<? extends EntryEditorTabModel> models) {
        int lastFieldSet = -1;
        for (int i = 0; i < models.size(); i++) {
            if (models.get(i) instanceof BuiltInTab(BuiltIn type, boolean ignored) && type.isFieldSet()) {
                lastFieldSet = i;
            }
        }
        return lastFieldSet >= 0 ? lastFieldSet + 1 : indexAfterLeadingPreview(models);
    }

    /// Every fixed tab in the entry editor, in display order: the leading {@link #PREVIEW}, then the built-in
    /// field-set tabs ({@link #isFieldSet()}), then the feature tabs.
    enum BuiltIn {
        // Always-present leading tab (no field configuration; only visibility)
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
        // Always-present trailing tabs
        MATH_SCI_NET,
        SOURCE,
        FULLTEXT_SEARCH_RESULTS;

        private static final Set<BuiltIn> FIELD_SETS = Set.of(
                REQUIRED_FIELDS, IMPORTANT_OPTIONAL_FIELDS, DETAIL_OPTIONAL_FIELDS, DEPRECATED_FIELDS, OTHER_FIELDS);

        /// Whether this is a built-in field-set tab (dynamic fields from the entry type) rather than a
        /// feature tab. Built-in field-set tabs always precede the feature tabs, and customized field-set
        /// tabs are inserted right after them.
        public boolean isFieldSet() {
            return FIELD_SETS.contains(this);
        }
    }

    record BuiltInTab(BuiltIn type, boolean visible)
            implements EntryEditorTabModel {
        @Override
        public Optional<BuiltIn> key() {
            return Optional.of(type);
        }

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
        public Optional<BuiltIn> key() {
            return Optional.empty();
        }

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
