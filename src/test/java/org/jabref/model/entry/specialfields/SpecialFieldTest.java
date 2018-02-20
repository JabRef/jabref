package org.jabref.model.entry.specialfields;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpecialFieldTest {


    @Test
    public void getSpecialFieldInstanceFromFieldNameValid() {
        assertEquals(Optional.of(SpecialField.RANKING),
                SpecialField.getSpecialFieldInstanceFromFieldName("ranking"));
    }

    @Test
    public void getSpecialFieldInstanceFromFieldNameEmptyForInvalidField() {
        assertEquals(Optional.empty(), SpecialField.getSpecialFieldInstanceFromFieldName("title"));
    }

    @Test
    public void isSpecialFieldTrueForValidField() {
        assertTrue(SpecialField.isSpecialField("ranking"));
    }

    @Test
    public void isSpecialFieldFalseForInvalidField() {
        assertFalse(SpecialField.isSpecialField("title"));
    }
}
