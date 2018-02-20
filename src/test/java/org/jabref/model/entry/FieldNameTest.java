package org.jabref.model.entry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldNameTest {

    @Test
    public void testOrFieldsTwoTerms() {
        assertEquals("aaa/bbb", FieldName.orFields("aaa", "bbb"));
    }

    @Test
    public void testOrFieldsThreeTerms() {
        assertEquals("aaa/bbb/ccc", FieldName.orFields("aaa", "bbb", "ccc"));
    }

}
