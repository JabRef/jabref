package org.jabref.gui.entryeditor;

import java.util.List;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldListSectionsTest {

    @Test
    void partitionGroupsFieldsIntoSectionsKeepingOrder() {
        List<Field> ordered = List.of(
                InternalField.KEY_FIELD,
                StandardField.AUTHOR,
                StandardField.DOI,
                StandardField.TITLE,
                StandardField.FILE,
                StandardField.COMMENT,
                StandardField.ISBN);

        List<FieldListSections.Section> sections = FieldListSections.partition(ordered);

        assertEquals(List.of(
                        FieldListSections.SectionType.MAIN,
                        FieldListSections.SectionType.IDENTIFIERS,
                        FieldListSections.SectionType.FILES_AND_LINKS,
                        FieldListSections.SectionType.COMMENTS),
                sections.stream().map(FieldListSections.Section::type).toList());
        assertEquals(List.of(InternalField.KEY_FIELD, StandardField.AUTHOR, StandardField.TITLE),
                List.copyOf(sections.get(0).fields()));
        assertEquals(List.of(StandardField.DOI, StandardField.ISBN),
                List.copyOf(sections.get(1).fields()));
        assertEquals(List.of(StandardField.FILE),
                List.copyOf(sections.get(2).fields()));
        assertEquals(List.of(StandardField.COMMENT),
                List.copyOf(sections.get(3).fields()));
    }

    @Test
    void emptySectionsAreOmitted() {
        List<FieldListSections.Section> sections =
                FieldListSections.partition(List.of(StandardField.AUTHOR, StandardField.TITLE));

        assertEquals(List.of(FieldListSections.SectionType.MAIN),
                sections.stream().map(FieldListSections.Section::type).toList());
    }

    @Test
    void userSpecificCommentFieldBelongsToCommentsSection() {
        assertEquals(FieldListSections.SectionType.COMMENTS,
                FieldListSections.sectionOf(new UserSpecificCommentField("koppor")));
    }

    @Test
    void subtractKeepsCandidateOrderAndRemovesShownFields() {
        assertEquals(List.of(StandardField.EDITOR, StandardField.SERIES),
                List.copyOf(FieldListSections.subtract(
                        List.of(StandardField.EDITOR, StandardField.VOLUME, StandardField.SERIES),
                        List.of(StandardField.VOLUME, StandardField.AUTHOR))));
    }

    @Test
    void fieldsOfIdentifiersStartsWithDoiAndCollectsAllIdentifierFields() {
        List<Field> identifierFields = List.copyOf(
                FieldListSections.fieldsOf(FieldListSections.SectionType.IDENTIFIERS));

        assertEquals(StandardField.DOI, identifierFields.getFirst());
        assertEquals(9, identifierFields.size());
    }

    @Test
    void urlAndUrlDateBelongToFilesAndLinksSection() {
        assertEquals(FieldListSections.SectionType.FILES_AND_LINKS,
                FieldListSections.sectionOf(StandardField.URL));
        assertEquals(FieldListSections.SectionType.FILES_AND_LINKS,
                FieldListSections.sectionOf(StandardField.URLDATE));
    }
}
