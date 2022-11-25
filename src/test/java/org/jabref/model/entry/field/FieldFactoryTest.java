package org.jabref.model.entry.field;

import org.jabref.model.entry.types.BiblatexApaEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FieldFactoryTest {
    @Test
    void testOrFieldsTwoTerms() {
        assertEquals("aaa/bbb", FieldFactory.serializeOrFields(new UnknownField("aaa"), new UnknownField("bbb")));
    }

    @Test
    void testOrFieldsThreeTerms() {
        assertEquals("aaa/bbb/ccc", FieldFactory.serializeOrFields(new UnknownField("aaa"), new UnknownField("bbb"), new UnknownField("ccc")));
    }

    @Test
    void testDoesNotParseApaFieldWithoutEntryType() {
        assertNotEquals(BiblatexApaField.ARTICLE, FieldFactory.parseField("article"));
    }

    @Test
    void testDoesParseApaFieldWithEntryType() {
        assertEquals(BiblatexApaField.ARTICLE, FieldFactory.parseField(BiblatexApaEntryType.Constitution, "article"));
    }
}
