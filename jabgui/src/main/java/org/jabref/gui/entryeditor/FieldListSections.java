package org.jabref.gui.entryeditor;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.SequencedSet;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;

/// Partitions the ordered field list of the {@link AllFieldsTab} into display sections
/// (Google-Contacts-style grouping, issue #12711). Plain Java (no JavaFX) so the
/// partition logic is unit-testable.
public final class FieldListSections {

    /// Declaration order == display order in the scroll list.
    public enum SectionType {
        /// Everything that is not in one of the special groups; rendered without a header.
        MAIN,
        IDENTIFIERS,
        FILES_AND_LINKS,
        COMMENTS;

        /// The localized section heading; empty for {@link #MAIN}.
        public Optional<String> header() {
            return switch (this) {
                case MAIN ->
                        Optional.empty();
                case IDENTIFIERS ->
                        Optional.of(Localization.lang("Identifiers"));
                case FILES_AND_LINKS ->
                        Optional.of(Localization.lang("Files and links"));
                case COMMENTS ->
                        Optional.of(Localization.lang("Comments"));
            };
        }
    }

    public record Section(SectionType type, SequencedSet<Field> fields) {
    }

    /// `FieldProperty.IDENTIFIER` only marks DOI/EPRINT/PMID, therefore an explicit list.
    /// Order = chip/display order in the Identifiers section.
    private static final SequencedSet<Field> IDENTIFIER_FIELDS = new LinkedHashSet<>(List.of(
            StandardField.DOI,
            StandardField.ISBN,
            StandardField.ISSN,
            StandardField.EPRINT,
            StandardField.EPRINTTYPE,
            StandardField.EPRINTCLASS,
            StandardField.ARCHIVEPREFIX,
            StandardField.PMID,
            StandardField.MR_NUMBER));

    /// Order = chip/display order in the Files and links section.
    private static final SequencedSet<Field> FILE_AND_LINK_FIELDS = new LinkedHashSet<>(List.of(
            StandardField.FILE,
            StandardField.URL,
            StandardField.URI,
            StandardField.URLDATE));

    private FieldListSections() {
    }

    /// All fields belonging to the given section (in display order) — the basis for the
    /// section's add-chips. Empty for {@link SectionType#MAIN} (its add-chips come from the
    /// entry type's optional fields) and {@link SectionType#COMMENTS} (user-specific comment
    /// fields are dynamic).
    public static SequencedSet<Field> fieldsOf(SectionType type) {
        return switch (type) {
            case IDENTIFIERS ->
                    new LinkedHashSet<>(IDENTIFIER_FIELDS);
            case FILES_AND_LINKS ->
                    new LinkedHashSet<>(FILE_AND_LINK_FIELDS);
            case MAIN, COMMENTS ->
                    new LinkedHashSet<>();
        };
    }

    public static SectionType sectionOf(Field field) {
        if (IDENTIFIER_FIELDS.contains(field)) {
            return SectionType.IDENTIFIERS;
        }
        if (FILE_AND_LINK_FIELDS.contains(field)) {
            return SectionType.FILES_AND_LINKS;
        }
        if (StandardField.COMMENT.equals(field) || (field instanceof UserSpecificCommentField)) {
            return SectionType.COMMENTS;
        }
        return SectionType.MAIN;
    }

    /// The candidates (in their given order) that are not part of `shown` — the fields
    /// offered as add-chips below the list.
    public static SequencedSet<Field> subtract(SequencedCollection<Field> candidates, Collection<Field> shown) {
        SequencedSet<Field> result = new LinkedHashSet<>(candidates);
        result.removeAll(shown);
        return result;
    }

    /// Splits `orderedFields` into sections, keeping the given order within each section.
    /// Empty sections are omitted; section order follows {@link SectionType} declaration order.
    public static List<Section> partition(SequencedCollection<Field> orderedFields) {
        Map<SectionType, SequencedSet<Field>> buckets = new EnumMap<>(SectionType.class);
        for (SectionType type : SectionType.values()) {
            buckets.put(type, new LinkedHashSet<>());
        }
        orderedFields.forEach(field -> buckets.get(sectionOf(field)).add(field));
        return Arrays.stream(SectionType.values())
                     .map(type -> new Section(type, buckets.get(type)))
                     .filter(section -> !section.fields().isEmpty())
                     .toList();
    }
}
