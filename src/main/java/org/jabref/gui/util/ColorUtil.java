package org.jabref.gui.util;

import javafx.scene.paint.Color;

public class ColorUtil {

    public static String toRGBCode(Color color) {
        return "#%02X%02X%02X".formatted(
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
        return "#%02x%02x%02x".formatted((int) validFieldBackgroundColor.getRed(), (int) validFieldBackgroundColor.getGreen(), (int) validFieldBackgroundColor.getBlue());
    }
}
