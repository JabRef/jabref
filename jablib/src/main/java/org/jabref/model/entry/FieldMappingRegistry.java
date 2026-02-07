package org.jabref.model.entry;

import java.util.Optional;
import java.util.Set;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

///
/// Registry for BibLaTeX cross-reference field inheritance rules.
/// The rules are based on the BibLaTeX documentation for cross-referencing entries.
///
public final class FieldMappingRegistry {

    // Fields that should never be inherited from a cross-referenced entry.
    private static final Set<Field> FORBIDDEN_FIELDS = Set.of(
            StandardField.IDS,
            StandardField.CROSSREF,
            StandardField.XREF,
            StandardField.ENTRYSET,
            StandardField.RELATED,
            StandardField.SORTKEY
    );

    ///
    /// Fields that block same-name inheritance for certain entry type pairs.
    /// When these fields are requested, they return empty (no inheritance).
    ///
    private static final Set<Field> TITLE_FIELDS = Set.of(
            StandardField.TITLE,
            StandardField.SUBTITLE,
            StandardField.TITLEADDON
    );

    private static final Set<Field> JOURNAL_TITLE_FIELDS = Set.of(
            StandardField.TITLE,
            StandardField.SUBTITLE
    );

    private FieldMappingRegistry() {
    }

    ///
    /// Maps an (empty) field of a BibEntry to a field of a cross-referenced entry.
    ///
    /// @param targetField field name of the BibEntry
    /// @param targetEntry type of the BibEntry
    /// @param sourceEntry type of the cross-referenced BibEntry
    /// @return the mapped field or empty if there is no valid mapping available
    ///
    public static Optional<Field> getSourceField(Field targetField, EntryType targetEntry, EntryType sourceEntry) {
        // 1. Sort out forbidden fields
        if (FORBIDDEN_FIELDS.contains(targetField)) {
            return Optional.empty();
        }

        // 2. Handle special field mappings (MvBook/Book -> InBook/BookInBook/SuppBook)
        if (isBookToInBookMapping(sourceEntry, targetEntry)) {
            if (targetField == StandardField.AUTHOR || targetField == StandardField.BOOKAUTHOR) {
                return Optional.of(StandardField.AUTHOR);
            }
        }

        // 3. Check for main title mapping (Mv* -> child types)
        if (isMvToChildMapping(sourceEntry, targetEntry)) {
            if (targetField == StandardField.MAINTITLE) {
                return Optional.of(StandardField.TITLE);
            }
            if (targetField == StandardField.MAINSUBTITLE) {
                return Optional.of(StandardField.SUBTITLE);
            }
            if (targetField == StandardField.MAINTITLEADDON) {
                return Optional.of(StandardField.TITLEADDON);
            }
            // those fields are no more available for the same-name inheritance strategy
            if (TITLE_FIELDS.contains(targetField)) {
                return Optional.empty();
            }
            // for these fields, inheritance is not allowed for the specified entry types
            if (targetField == StandardField.SHORTTITLE) {
                return Optional.empty();
            }
        }

        // 4. Check for book title mapping (container -> In* types)
        if (isContainerToInMapping(sourceEntry, targetEntry)) {
            if (targetField == StandardField.BOOKTITLE) {
                return Optional.of(StandardField.TITLE);
            }
            if (targetField == StandardField.BOOKSUBTITLE) {
                return Optional.of(StandardField.SUBTITLE);
            }
            if (targetField == StandardField.BOOKTITLEADDON) {
                return Optional.of(StandardField.TITLEADDON);
            }
            // those fields are no more available for the same-name inheritance strategy
            if (TITLE_FIELDS.contains(targetField)) {
                return Optional.empty();
            }
            // for these fields, inheritance is not allowed for the specified entry types
            if (targetField == StandardField.SHORTTITLE) {
                return Optional.empty();
            }
        }

        // 5. Check for journal title mapping (Periodical -> Article/SuppPeriodical)
        if (isPeriodicalToArticleMapping(sourceEntry, targetEntry)) {
            if (targetField == StandardField.JOURNALTITLE) {
                return Optional.of(StandardField.TITLE);
            }
            if (targetField == StandardField.JOURNALSUBTITLE) {
                return Optional.of(StandardField.SUBTITLE);
            }
            // those fields are no more available for the same-name inheritance strategy
            if (JOURNAL_TITLE_FIELDS.contains(targetField)) {
                return Optional.empty();
            }
            // for these fields, inheritance is not allowed for the specified entry types
            if (targetField == StandardField.SHORTTITLE) {
                return Optional.empty();
            }
        }

        // 6. Fallback to inherit the field with the same name
        return Optional.ofNullable(targetField);
    }

    private static boolean isBookToInBookMapping(EntryType sourceEntry, EntryType targetEntry) {
        return ((sourceEntry == StandardEntryType.MvBook) || (sourceEntry == StandardEntryType.Book)) &&
                ((targetEntry == StandardEntryType.InBook) ||
                        (targetEntry == StandardEntryType.BookInBook) ||
                        (targetEntry == StandardEntryType.SuppBook));
    }

    private static boolean isMvToChildMapping(EntryType sourceEntry, EntryType targetEntry) {
        return isMvBookToBookOrChild(sourceEntry, targetEntry) ||
                isMvCollectionToChild(sourceEntry, targetEntry) ||
                isMvProceedingsToChild(sourceEntry, targetEntry) ||
                isMvReferenceToChild(sourceEntry, targetEntry);
    }

    private static boolean isMvBookToBookOrChild(EntryType sourceEntry, EntryType targetEntry) {
        return (sourceEntry == StandardEntryType.MvBook) &&
                ((targetEntry == StandardEntryType.Book) ||
                        (targetEntry == StandardEntryType.InBook) ||
                        (targetEntry == StandardEntryType.BookInBook) ||
                        (targetEntry == StandardEntryType.SuppBook));
    }

    private static boolean isMvCollectionToChild(EntryType sourceEntry, EntryType targetEntry) {
        return (sourceEntry == StandardEntryType.MvCollection) &&
                ((targetEntry == StandardEntryType.Collection) ||
                        (targetEntry == StandardEntryType.InCollection) ||
                        (targetEntry == StandardEntryType.SuppCollection));
    }

    private static boolean isMvProceedingsToChild(EntryType sourceEntry, EntryType targetEntry) {
        return (sourceEntry == StandardEntryType.MvProceedings) &&
                ((targetEntry == StandardEntryType.Proceedings) ||
                        (targetEntry == StandardEntryType.InProceedings));
    }

    private static boolean isMvReferenceToChild(EntryType sourceEntry, EntryType targetEntry) {
        return (sourceEntry == StandardEntryType.MvReference) &&
                ((targetEntry == StandardEntryType.Reference) ||
                        (targetEntry == StandardEntryType.InReference));
    }

    private static boolean isContainerToInMapping(EntryType sourceEntry, EntryType targetEntry) {
        return isBookToInBook(sourceEntry, targetEntry) ||
                isCollectionToInCollection(sourceEntry, targetEntry) ||
                isReferenceToInReference(sourceEntry, targetEntry) ||
                isProceedingsToInProceedings(sourceEntry, targetEntry);
    }

    private static boolean isBookToInBook(EntryType sourceEntry, EntryType targetEntry) {
        return (sourceEntry == StandardEntryType.Book) &&
                ((targetEntry == StandardEntryType.InBook) ||
                        (targetEntry == StandardEntryType.BookInBook) ||
                        (targetEntry == StandardEntryType.SuppBook));
    }

    private static boolean isCollectionToInCollection(EntryType sourceEntry, EntryType targetEntry) {
        return (sourceEntry == StandardEntryType.Collection) &&
                ((targetEntry == StandardEntryType.InCollection) ||
                        (targetEntry == StandardEntryType.SuppCollection));
    }

    private static boolean isReferenceToInReference(EntryType sourceEntry, EntryType targetEntry) {
        return (sourceEntry == StandardEntryType.Reference) &&
                (targetEntry == StandardEntryType.InReference);
    }

    private static boolean isProceedingsToInProceedings(EntryType sourceEntry, EntryType targetEntry) {
        return (sourceEntry == StandardEntryType.Proceedings) &&
                (targetEntry == StandardEntryType.InProceedings);
    }

    private static boolean isPeriodicalToArticleMapping(EntryType sourceEntry, EntryType targetEntry) {
        return (sourceEntry == IEEETranEntryType.Periodical) &&
                ((targetEntry == StandardEntryType.Article) ||
                        (targetEntry == StandardEntryType.SuppPeriodical));
    }
}
