package org.jabref.gui.validation;

import javafx.scene.control.Label;

import org.jabref.gui.icon.IconTheme;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ApplicationExtension.class)
class ValidationVisualizerTest {

    static Object[][] iconTestData() {
        return new Object[][] {
                {Severity.ERROR, IconTheme.JabRefIcons.ERROR.getGraphicNode().toString()},
                {Severity.WARNING, IconTheme.JabRefIcons.WARNING.getGraphicNode().toString()}
        };
    }

    @ParameterizedTest
    @MethodSource("iconTestData")
    void applyIconPicksGraphicBySeverity(Severity severity, String expectedGraphic) {
        ValidationVisualizer visualizer = new ValidationVisualizer();
        Label icon = new Label();

        visualizer.applyIcon(icon, new ValidationMessage(severity, "test"));

        assertEquals(expectedGraphic, icon.getGraphic().toString());
    }
}
