package org.jabref.gui.groups;

import javafx.scene.input.Dragboard;

import org.jabref.gui.util.CustomLocalDragboard;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class GroupTreeViewTest {

    @Test
    void fileDropDoesNotAssignStaleLocalEntries() {
        Dragboard dragboard = mock(Dragboard.class);
        CustomLocalDragboard localDragboard = mock(CustomLocalDragboard.class);

        when(dragboard.hasFiles()).thenReturn(true);
        when(localDragboard.hasBibEntries()).thenReturn(true);

        assertFalse(GroupTreeView.shouldAssignLocalEntries(dragboard, localDragboard));
    }

    @Test
    void localEntryDropAssignsEntriesWhenNoFilesArePresent() {
        Dragboard dragboard = mock(Dragboard.class);
        CustomLocalDragboard localDragboard = mock(CustomLocalDragboard.class);

        when(dragboard.hasFiles()).thenReturn(false);
        when(localDragboard.hasBibEntries()).thenReturn(true);

        assertTrue(GroupTreeView.shouldAssignLocalEntries(dragboard, localDragboard));
    }
}
