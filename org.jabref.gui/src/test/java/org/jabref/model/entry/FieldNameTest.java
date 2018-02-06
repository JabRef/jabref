package org.jabref.model.entry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


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
