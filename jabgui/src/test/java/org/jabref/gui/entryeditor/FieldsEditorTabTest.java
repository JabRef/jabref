package org.jabref.gui.entryeditor;

import java.util.HashMap;
import java.util.Map;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldsEditorTabTest {

    @BeforeEach
    void clearCache() {
        FieldsEditorTab.CARET_POSITIONS.clear();
    }

    private static void remember(int entryId) {
        Map<Field, Integer> positions = new HashMap<>();
        positions.put(StandardField.TITLE, entryId);
        FieldsEditorTab.CARET_POSITIONS.put(String.valueOf(entryId), positions);
    }

    @Test
    void cacheDropsLeastRecentlyUsedEntryWhenFull() {
        for (int entryId = 0; entryId <= FieldsEditorTab.CARET_POSITIONS_MAX_ENTRIES; entryId++) {
            remember(entryId);
        }

        assertEquals(FieldsEditorTab.CARET_POSITIONS_MAX_ENTRIES, FieldsEditorTab.CARET_POSITIONS.size());
        assertFalse(FieldsEditorTab.CARET_POSITIONS.containsKey("0"));
    }

    @Test
    void readingAnEntryKeepsItInTheCache() {
        for (int entryId = 0; entryId < FieldsEditorTab.CARET_POSITIONS_MAX_ENTRIES; entryId++) {
            remember(entryId);
        }

        // same lookup restoreCaretPositions() does - it has to count as a use
        FieldsEditorTab.CARET_POSITIONS.getOrDefault("0", Map.of());
        remember(FieldsEditorTab.CARET_POSITIONS_MAX_ENTRIES);

        assertTrue(FieldsEditorTab.CARET_POSITIONS.containsKey("0"));
        assertFalse(FieldsEditorTab.CARET_POSITIONS.containsKey("1"));
    }
}
