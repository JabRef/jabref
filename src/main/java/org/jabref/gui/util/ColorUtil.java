package org.jabref.gui.util;

import javafx.scene.paint.Color;

public class ColorUtil {
    public static java.awt.Color toAWT(Color color) {
        return new java.awt.Color((float) color.getRed(),
                (float) color.getGreen(),
                (float) color.getBlue(),
                (float) color.getOpacity());
    }

    public static Color toFX(java.awt.Color awtColor) {
        int r = awtColor.getRed();
        int g = awtColor.getGreen();
        int b = awtColor.getBlue();
        int a = awtColor.getAlpha();
        double opacity = a / 255.0;
        return Color.rgb(r, g, b, opacity);
    }

    public static String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    public static String toHex(java.awt.Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
