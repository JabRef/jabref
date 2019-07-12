package org.jabref.model.entry;

import org.jabref.model.entry.field.FieldFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StandardFieldTest {

    @Test
    public void testOrFieldsTwoTerms() {
        assertEquals("aaa/bbb", FieldFactory.serializeOrFields("aaa", "bbb"));
    }

    @Test
    public void testOrFieldsThreeTerms() {
        assertEquals("aaa/bbb/ccc", FieldFactory.serializeOrFields("aaa", "bbb", "ccc"));
    }
}
