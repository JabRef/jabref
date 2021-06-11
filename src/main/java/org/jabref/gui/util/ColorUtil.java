package org.jabref.gui.util;

import javafx.scene.paint.Color;

public class ColorUtil {

    public static String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    public static String toRGBACode(Color color) {
        return String.format("rgba(%d,%d,%d,%f)",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                color.getOpacity());
    }

    public static String toHex(Color validFieldBackgroundColor) {
        return String.format("#%02x%02x%02x", (int) validFieldBackgroundColor.getRed(), (int) validFieldBackgroundColor.getGreen(), (int) validFieldBackgroundColor.getBlue());
    }
}
