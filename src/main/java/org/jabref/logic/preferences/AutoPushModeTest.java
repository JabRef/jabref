package org.jabref.logic.preferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AutoPushModeTest {

    @Test
    void allEnumValuesExist() {
        assertNotNull(AutoPushMode.MANUALLY);
        assertNotNull(AutoPushMode.ON_SAVE);
    }

    @Test
    void getDisplayNameReturnsCorrectValue() {
        assertEquals("Manually", AutoPushMode.MANUALLY.getDisplayName());
        assertEquals("On Save", AutoPushMode.ON_SAVE.getDisplayName());
    }

    @Test
    void toStringReturnsDisplayName() {
        assertEquals("Manually", AutoPushMode.MANUALLY.toString());
        assertEquals("On Save", AutoPushMode.ON_SAVE.toString());
    }

    @Test
    void fromStringReturnsCorrectEnum() {
        assertEquals(AutoPushMode.MANUALLY, AutoPushMode.fromString("Manually"));
        assertEquals(AutoPushMode.ON_SAVE, AutoPushMode.fromString("On Save"));
    }

    @Test
    void fromStringIsCaseInsensitive() {
        assertEquals(AutoPushMode.MANUALLY, AutoPushMode.fromString("manually"));
        assertEquals(AutoPushMode.ON_SAVE, AutoPushMode.fromString("on save"));
    }

    @Test
    void fromStringInvalidReturnsManually() {
        assertEquals(AutoPushMode.MANUALLY, AutoPushMode.fromString("invalid"));
        assertEquals(AutoPushMode.MANUALLY, AutoPushMode.fromString(""));
        assertEquals(AutoPushMode.MANUALLY, AutoPushMode.fromString(null));
    }
}
