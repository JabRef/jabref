package org.jabref.gui.groups;

import java.util.List;
import java.util.stream.Stream;

import javafx.scene.paint.Color;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupColorPickerTest {

    static Stream<Arguments> generateColor() {
        return Stream.of(
                Arguments.of(
                        Color.hsb(180, 1.0, 1.0),
                        List.of(Color.hsb(0, 1.0, 1.0))
                ),
                Arguments.of(
                        Color.hsb(270, 1.0, 1.0),
                        List.of(
                                Color.hsb(0, 1.0, 1.0),
                                Color.hsb(180, 1.0, 1.0))
                ),
                Arguments.of(
                        Color.hsb(270, 1.0, 1.0),
                        List.of(
                                Color.hsb(0, 1.0, 1.0),
                                Color.hsb(90, 1.0, 1.0),
                                Color.hsb(180, 1.0, 1.0))
                )
        );
    }

    private static String colorToHsb(Color color) {
        return "h:%,.4f s:%,.4f b:%,.4f".formatted(color.getHue(), color.getSaturation(), color.getBrightness());
    }

    @ParameterizedTest
    @MethodSource
    void generateColor(Color expected, List<Color> subgroupColors) {
        Color result = GroupColorPicker.generateColor(subgroupColors);
        assertEquals(expected, result,
                "%s != %s".formatted(colorToHsb(expected), colorToHsb(result)));
    }
}
