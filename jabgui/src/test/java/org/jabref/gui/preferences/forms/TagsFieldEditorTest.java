package org.jabref.gui.preferences.forms;

import java.util.List;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagsFieldEditorTest {

    private static final List<Field> CANDIDATES = List.of(StandardField.TITLE, StandardField.AUTHOR, StandardField.JOURNALTITLE);

    @Test
    void suggestsMatchingCandidatesSortedByName() {
        assertEquals(List.of(StandardField.JOURNALTITLE, StandardField.TITLE),
                TagsFieldEditor.fieldSuggestions(CANDIDATES, "title"));
    }

    @Test
    void offersTypedTextAsFieldOfItsOwnFirst() {
        assertEquals(List.of(FieldFactory.parseField("tit"), StandardField.JOURNALTITLE, StandardField.TITLE),
                TagsFieldEditor.fieldSuggestions(CANDIDATES, "tit"));
    }

    @Test
    void suggestsAllCandidatesForEmptyText() {
        assertEquals(List.of(StandardField.AUTHOR, StandardField.JOURNALTITLE, StandardField.TITLE),
                TagsFieldEditor.fieldSuggestions(CANDIDATES, ""));
    }
}
