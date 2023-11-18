package org.jabref.model.entry.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class SpecialFieldTest {

    @Test
    public void getSpecialFieldInstanceFromFieldNameValid() {
        assertEquals(Optional.of(SpecialField.RANKING), SpecialField.fromName("ranking"));
    }

    @Test
    public void getSpecialFieldInstanceFromFieldNameEmptyForInvalidField() {
        assertEquals(Optional.empty(), SpecialField.fromName("title"));
    }
}
