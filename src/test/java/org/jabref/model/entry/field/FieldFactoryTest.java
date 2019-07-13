package org.jabref.model.entry.field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldFactoryTest {
    @Test
    void testOrFieldsTwoTerms() {
        assertEquals("aaa/bbb", FieldFactory.serializeOrFields(new UnknownField("aaa"), new UnknownField("bbb")));
    }

    @Test
    void testOrFieldsThreeTerms() {
        assertEquals("aaa/bbb/ccc", FieldFactory.serializeOrFields(new UnknownField("aaa"), new UnknownField("bbb"), new UnknownField("ccc")));
    }
}
