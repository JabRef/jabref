package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Set;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

/// Discriminated union of the kinds of tabs that appear in the entry editor.
///
/// {@link BuiltInTab}         — a fixed tab identified by a {@link BuiltIn} constant.
/// {@link CustomizedFieldsTab} — user-customizable tab backed by an explicit, named set of fields.
public sealed interface EntryEditorTabModel
        permits EntryEditorTabModel.BuiltInTab, EntryEditorTabModel.CustomizedFieldsTab {

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
        SOURCE,
        FULLTEXT_SEARCH_RESULTS;

        private static final Set<BuiltIn> FIELD_SETS = Set.of(
                REQUIRED_FIELDS, IMPORTANT_OPTIONAL_FIELDS, DETAIL_OPTIONAL_FIELDS, DEPRECATED_FIELDS, OTHER_FIELDS);

        // Create displayName dynamically for language preference change
        public String displayName() {
            return switch (this) {
                case PREVIEW ->
                        Localization.lang("Preview");
                case REQUIRED_FIELDS ->
                        Localization.lang("Required fields");
                case IMPORTANT_OPTIONAL_FIELDS ->
                        Localization.lang("Optional fields");
                case DETAIL_OPTIONAL_FIELDS ->
                        Localization.lang("Optional fields 2");
                case DEPRECATED_FIELDS ->
                        Localization.lang("Deprecated fields");
                case OTHER_FIELDS ->
                        Localization.lang("Other fields");
                case RELATED_ARTICLES ->
                        Localization.lang("Related articles");
                case AI_SUMMARY ->
                        Localization.lang("AI summary");
                case AI_CHAT ->
                        Localization.lang("AI chat");
                case FILE_ANNOTATIONS ->
                        Localization.lang("File annotations");
                case LATEX_CITATIONS ->
                        Localization.lang("LaTeX citations");
                case CITATION_INFORMATION ->
                        Localization.lang("Citations");
                case COMMENTS ->
                        Localization.lang("Comments");
                case SOURCE ->
                        Localization.lang("Source");
                case FULLTEXT_SEARCH_RESULTS ->
                        Localization.lang("Fulltext search results");
            };
        }

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
    record CustomizedFieldsTab(String name, Set<Field> fields)
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
