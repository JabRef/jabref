package org.jabref.logic.openoffice;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class OpenOfficePreferencesTest {

    @Test
    void addSpaceAfterDefaultsToFalseAndCanBeChanged() {
        OpenOfficePreferences preferences = OpenOfficePreferences.getDefault();

        assertFalse(preferences.getAddSpaceAfter());

        preferences.setAddSpaceAfter(true);

        assertTrue(preferences.getAddSpaceAfter());
    }
}
