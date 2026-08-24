package org.jabref.gui.entryeditor;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

/// Model of the tabs that appear in the entry editor.
///
/// [BuiltInTab] — a fixed tab (the single scroll-list "Main" tab of issue #12711 and the
/// feature tabs) identified by a [BuiltIn] constant; users only toggle visibility and order.
/// [CustomizedFieldsTab] — a user-defined tab backed by a named, ordered list of field patterns.
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

    /// A user-defined tab showing an explicit, ordered list of field patterns. Always shown while its
    /// pattern list resolves to at least one field; toggled only by being added to or removed from the
    /// tab list, so it carries no visibility flag (unlike [BuiltInTab]).
    ///
    /// A pattern is either a plain field name (always shown, even while unset on the entry) or a
    /// regular expression (contains regex metacharacters), which captures every *set* field of the
    /// entry whose name matches it — e.g. `comment-.*` for all user-specific comment fields.
    record CustomizedFieldsTab(String name, List<String> fieldPatterns)
            implements EntryEditorTabModel {

        /// A pattern without any regex metacharacter is a plain field name.
        private static final Pattern PLAIN_FIELD_NAME = Pattern.compile("[^\\\\^$.|?*+()\\[\\]{}]+");

        public CustomizedFieldsTab {
            fieldPatterns = List.copyOf(fieldPatterns);
        }

        /// The former default tabs "General" and "Abstract" of JabRef 5, for users who prefer these fields
        /// split off from the "Main" tab (which shows them as well).
        public static List<CustomizedFieldsTab> classicTabs() {
            List<String> generalFields = Stream.concat(
                                                       Stream.of(StandardField.DOI, StandardField.ICORERANKING, StandardField.CITATIONCOUNT, StandardField.CROSSREF,
                                                               StandardField.KEYWORDS, StandardField.EPRINT, StandardField.EPRINTTYPE, StandardField.URL,
                                                               StandardField.FILE, StandardField.GROUPS, StandardField.OWNER, StandardField.TIMESTAMP),
                                                       EnumSet.allOf(SpecialField.class).stream())
                                               .map(Field::getName)
                                               .toList();
            return List.of(
                    new CustomizedFieldsTab(Localization.lang("General"), generalFields),
                    new CustomizedFieldsTab(Localization.lang("Abstract"), List.of(StandardField.ABSTRACT.getName())));
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public EntryEditorTabModel withVisible(boolean visible) {
            return this;
        }

        /// Resolves [#fieldPatterns] against the given entry, keeping the configured pattern order;
        /// the fields captured by one regex pattern are sorted by name. Invalid regexes resolve to nothing.
        // [impl->req~entry-editor.custom-tabs~1]
        public SequencedSet<Field> resolveFields(BibEntry entry) {
            SequencedSet<Field> result = new LinkedHashSet<>();
            for (String pattern : fieldPatterns) {
                if (PLAIN_FIELD_NAME.matcher(pattern).matches()) {
                    result.add(FieldFactory.parseField(entry.getType(), pattern));
                    continue;
                }
                try {
                    Pattern compiled = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                    entry.getFields().stream()
                         .filter(field -> compiled.matcher(field.getName()).matches())
                         .sorted(Comparator.comparing(Field::getName))
                         .forEach(result::add);
                } catch (PatternSyntaxException _) {
                    // Invalid regex: resolves to no fields; the preferences UI accepts any string, so
                    // a broken pattern must not break rendering the tab.
                }
            }
            return result;
        }
    }
}
