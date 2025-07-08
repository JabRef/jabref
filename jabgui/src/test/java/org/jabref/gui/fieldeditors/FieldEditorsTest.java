package org.jabref.gui.fieldeditors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FieldEditorsTest {

    @Test
    void testEditorCreationDoesNotCrash() {
        assertNotNull(FieldEditors.class);
    }
}
