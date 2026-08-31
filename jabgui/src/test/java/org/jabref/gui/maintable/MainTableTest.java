package org.jabref.gui.maintable;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
@ExtendWith(ApplicationExtension.class)
class MainTableTest {

    @Test
    void centerSelectedEntryKeepsAlreadyCenteredSelectionCentered() {
        assertEquals(35, MainTable.getCenteredTopIndex(45, 20, 100));
    }

    @Test
    void centerSelectedEntryAboveCurrentViewportScrollsUpToCenter() {
        assertEquals(15, MainTable.getCenteredTopIndex(25, 20, 100));
    }

    @Test
    void centerSelectedEntryBelowCurrentViewportScrollsDownToCenter() {
        assertEquals(65, MainTable.getCenteredTopIndex(75, 20, 100));
    }

    @Test
    void centerSelectedEntryNearTopClampsToFirstRow() {
        assertEquals(0, MainTable.getCenteredTopIndex(5, 20, 100));
    }

    @Test
    void centerSelectedEntryNearBottomClampsToLastFullViewport() {
        assertEquals(80, MainTable.getCenteredTopIndex(95, 20, 100));
    }
}
