package org.jabref.gui.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public class ColorUtil {

    public static String toRGBCode(Color color) {
        return "#%02X%02X%02X".formatted(
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    public static String toRGBACode(Color color) {
        return "rgba(%d,%d,%d,%f)".formatted(
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                color.getOpacity());
    }

    public static String toHex(Color validFieldBackgroundColor) {
        return "#%02x%02x%02x".formatted((int) validFieldBackgroundColor.getRed(), (int) validFieldBackgroundColor.getGreen(), (int) validFieldBackgroundColor.getBlue());
    }

    public static StringProperty createFlashingColorStringProperty(final ObjectProperty<Color> flashingColor) {
        final StringProperty flashingColorStringProperty = new SimpleStringProperty();
        setColorStringFromColor(flashingColorStringProperty, flashingColor);
        flashingColor.addListener((observable, oldValue, newValue) -> setColorStringFromColor(flashingColorStringProperty, flashingColor));
        return flashingColorStringProperty;
    }

    public static void setColorStringFromColor(StringProperty colorStringProperty, ObjectProperty<Color> color) {
        colorStringProperty.set(ColorUtil.toRGBACode(color.get()));
    }
}
