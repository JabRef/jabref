package org.jabref.gui.util;

import javafx.scene.paint.Color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColorUtilTest {

    private ColorUtil colorUtil = new ColorUtil();
    private final Color c1 = Color.color(0.2, 0.4, 1);
    private final Color c2 = Color.rgb(255, 255, 255);

    @Test
    public void toRGBCodeTest() {
        assertEquals("#3366FF", ColorUtil.toRGBCode(c1));
        assertEquals("#FFFFFF", ColorUtil.toRGBCode(c2));
    }

    @Test
    public void toHexTest() {
        assertEquals("#000001", ColorUtil.toHex(c1));
        assertEquals("#010101", ColorUtil.toHex(c2));
    }
}
