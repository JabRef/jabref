package org.jabref.model.entry;

import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StandardFieldTest {

    @Test
    public void testOrFieldsTwoTerms() {
        assertEquals("aaa/bbb", FieldFactory.serializeOrFields(new UnknownField("aaa"), new UnknownField("bbb")));
    }

    @Test
    public void testOrFieldsThreeTerms() {
        assertEquals("aaa/bbb/ccc", FieldFactory.serializeOrFields(new UnknownField("aaa"), new UnknownField("bbb"), new UnknownField("ccc")));
    }
}
