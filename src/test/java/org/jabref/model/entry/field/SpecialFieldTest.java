package org.jabref.model.entry.field;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpecialFieldTest {

    @Test
    public void getSpecialFieldInstanceFromFieldNameValid() {
        assertEquals(Optional.of(SpecialField.RANKING),
                SpecialField.fromName("ranking"));
    }

    @Test
    public void getSpecialFieldInstanceFromFieldNameEmptyForInvalidField() {
        assertEquals(Optional.empty(), SpecialField.fromName("title"));
    }
}
