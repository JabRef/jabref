package org.jabref.gui.entryeditor;

import java.util.Set;

import org.jabref.logic.l10n.Localization;

/// Model of the tabs that appear in the entry editor.
///
/// Since the single scroll-list "Main" tab (issue #12711) replaced the classic category
/// tabs and the user-customizable field-set tabs, every tab is a fixed [BuiltInTab]
/// identified by a [BuiltIn] constant; users only toggle visibility.
public sealed interface EntryEditorTabModel
        permits EntryEditorTabModel.BuiltInTab {

    boolean isVisible();

    EntryEditorTabModel withVisible(boolean visible);

    default boolean isPreview() {
        return this instanceof BuiltInTab(
                BuiltIn type,
                boolean _
        ) && type == BuiltIn.PREVIEW;
    }

    /// Every fixed tab in the entry editor, in display order.
    enum BuiltIn {
        // Preview tab visibility controlled by preference option
        PREVIEW,

        // The single scroll-list tab showing all fields (issue #12711)
        ALL_FIELDS,

        // Feature tabs: fixed implementations
        RELATED_ARTICLES,
        AI_SUMMARY,
        AI_CHAT,
        FILE_ANNOTATIONS,
        LATEX_CITATIONS,
        CITATION_INFORMATION,
        SOURCE,
        FULLTEXT_SEARCH_RESULTS;

        private static final Set<BuiltIn> FIELD_SETS = Set.of(ALL_FIELDS);

        // Create displayName dynamically for language preference change
        public String displayName() {
            return switch (this) {
                case PREVIEW ->
                        Localization.lang("Preview");
                case ALL_FIELDS ->
                        Localization.lang("Main");
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
}
