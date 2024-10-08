package org.jabref.gui.util;

import java.util.stream.Stream;

import javafx.scene.paint.Color;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorUtilTest {

    private static final Color C1 = Color.color(0.2, 0.4, 1);
    private static final Color C2 = Color.rgb(255, 255, 255);
    private static final Color C3 = Color.color(0, 0, 0, 0);
    private static final Color C4 = Color.color(1, 1, 1, 1);
    private static final Color C5 = Color.color(0.6, 0.8, 0.5, 0.3);

    private ColorUtil colorUtil = new ColorUtil();

    @Test
    void toRGBCodeTest() {
        assertEquals("#3366FF", ColorUtil.toRGBCode(C1));
        assertEquals("#FFFFFF", ColorUtil.toRGBCode(C2));
    }

    @ParameterizedTest
    @MethodSource("provideToRGBACodeTest")
    void toRGBACodeTest(Color color, String expected) {
        assertEquals(expected, ColorUtil.toRGBACode(color));
    }

    private static Stream<Arguments> provideToRGBACodeTest() {
        return Stream.of(
                Arguments.of(C1, "rgba(51,102,255,%f)".formatted(1.0)),
                Arguments.of(C2, "rgba(255,255,255,%f)".formatted(1.0)),
                Arguments.of(C3, "rgba(0,0,0,%f)".formatted(0.0)),
                Arguments.of(C4, "rgba(255,255,255,%f)".formatted(1.0)),
                Arguments.of(C5, "rgba(153,204,127,%f)".formatted(0.3))
        );
    }

    @Test
    void toHexTest() {
        assertEquals("#000001", ColorUtil.toHex(C1));
        assertEquals("#010101", ColorUtil.toHex(C2));
    }
}
