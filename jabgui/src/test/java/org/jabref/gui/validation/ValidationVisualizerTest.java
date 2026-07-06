package org.jabref.gui.validation;

import java.util.stream.Stream;

import javafx.scene.control.Label;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class ValidationVisualizerTest {

    static Stream<Arguments> iconTestData() {
        return Stream.of(
                Arguments.of(Severity.ERROR, IconTheme.JabRefIcons.ERROR),
                Arguments.of(Severity.WARNING, IconTheme.JabRefIcons.WARNING)
        );
    }

    @ParameterizedTest
    @MethodSource("iconTestData")
    void applyIconPicksGraphicBySeverity(Severity severity, JabRefIcon expectedIcon) {
        ValidationVisualizer visualizer = new ValidationVisualizer();
        Label icon = new Label();

        visualizer.applyIcon(icon, new ValidationMessage(severity, "test"));

        assertTrue(expectedIcon.matches(icon.getGraphic()));
    }
}
