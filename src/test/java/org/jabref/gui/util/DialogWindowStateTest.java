package org.jabref.gui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DialogWindowStateTest {

    private final DialogWindowState dialogWindowState = new DialogWindowState(240, 300, 180, 360);

    @Test
    public void getXTest() {
        assertEquals(240, dialogWindowState.getX());
    }

    @Test
    public void getYTest() {
        assertEquals(300, dialogWindowState.getY());
    }

    @Test
    public void getHeightTest() {
        assertEquals(180, dialogWindowState.getHeight());
    }

    @Test
    public void getWidthTest() {
        assertEquals(360, dialogWindowState.getWidth());
    }
}
