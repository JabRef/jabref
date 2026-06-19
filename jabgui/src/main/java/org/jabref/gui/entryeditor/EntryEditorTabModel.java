package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Optional;
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

    /// Stable identity of a visibility-toggleable tab: its enum discriminator
    /// ({@link BuiltInFieldSet} for {@link FieldSet}, {@link StaticTab} for {@link Feature}). Lets callers
    /// look up or toggle a tab by key without switching on the record subtype.
    sealed interface TabKey permits StaticTab, BuiltInFieldSet {
    }

    /// This tab's key, or empty for {@link CustomizedFieldSet} (always visible, no toggle).
    Optional<TabKey> key();

    /// Whether this tab is currently shown. {@link CustomizedFieldSet} is always visible.
    boolean isVisible();

    /// A copy of this tab with the given visibility. No-op for {@link CustomizedFieldSet}.
    EntryEditorTabModel withVisible(boolean visible);

    /// Whether this is the always-present leading Preview feature tab (its visibility is owned by
    /// PreviewPreferences; customized field-set tabs are inserted right after it).
    default boolean isPreview() {
        return this instanceof Feature(
                StaticTab type,
                boolean ignored
        ) && type == StaticTab.PREVIEW;
    }

    /// Index just past a leading {@link StaticTab#PREVIEW} feature in {@code models} (1 if present, else 0),
    /// so customized field-set tabs are inserted after the Preview tab instead of before it.
    static int indexAfterLeadingPreview(List<? extends EntryEditorTabModel> models) {
        return !models.isEmpty() && models.getFirst().isPreview() ? 1 : 0;
    }

    /// The one canonical insertion point for customized field-set tabs: right after the built-in
    /// {@link FieldSet} tabs (so they sit between the built-in field sets and the feature tabs). This must
    /// match the persisted load order; otherwise saving silently reorders the tabs on every round-trip.
    /// Falls back to just after a leading Preview tab when no built-in field sets are present.
    static int indexAfterBuiltInFieldSets(List<? extends EntryEditorTabModel> models) {
        int lastFieldSet = -1;
        for (int i = 0; i < models.size(); i++) {
            if (models.get(i) instanceof FieldSet) {
                lastFieldSet = i;
            }
        }
        return lastFieldSet >= 0 ? lastFieldSet + 1 : indexAfterLeadingPreview(models);
    }

    enum StaticTab implements TabKey {
        // Always-present leading tab (no field configuration; only visibility)
        PREVIEW,
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
        FULLTEXT_SEARCH_RESULTS
    }

    enum BuiltInFieldSet implements TabKey {
        REQUIRED_FIELDS,
        IMPORTANT_OPTIONAL_FIELDS,
        DETAIL_OPTIONAL_FIELDS,
        DEPRECATED_FIELDS,
        OTHER_FIELDS
    }

    record FieldSet(BuiltInFieldSet type, boolean visible)
            implements EntryEditorTabModel {
        @Override
        public Optional<TabKey> key() {
            return Optional.of(type);
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public EntryEditorTabModel withVisible(boolean visible) {
            return new FieldSet(type, visible);
        }
    }

    /// Always shown; toggled only by being added to or removed from the tab list, so it carries no
    /// visibility flag (unlike {@link FieldSet} and {@link Feature}).
    record CustomizedFieldSet(String name, Set<Field> fields)
            implements EntryEditorTabModel {
        @Override
        public Optional<TabKey> key() {
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

    record Feature(StaticTab type, boolean visible)
            implements EntryEditorTabModel {
        @Override
        public Optional<TabKey> key() {
            return Optional.of(type);
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public EntryEditorTabModel withVisible(boolean visible) {
            return new Feature(type, visible);
        }
    }
}
