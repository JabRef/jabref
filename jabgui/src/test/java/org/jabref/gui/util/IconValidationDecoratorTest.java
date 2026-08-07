package org.jabref.gui.util;

import javafx.scene.control.Control;
import javafx.scene.control.Label;

import org.jabref.gui.icon.IconTheme;

import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationMessage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ApplicationExtension.class)
public class IconValidationDecoratorTest {
    static Object[][] decorationTestData() {
        return new Object[][] {
                {Severity.ERROR, IconTheme.JabRefIcons.ERROR.getGraphicNode().toString()},
                {Severity.WARNING, IconTheme.JabRefIcons.WARNING.getGraphicNode().toString()}
        };
    }

    @ParameterizedTest
    @MethodSource("decorationTestData")
    public void createDecorationNodeTest(Severity severity, String expectedGraphic) {
        IconValidationDecorator iconValidationDecorator = new IconValidationDecorator();
        Label node = (Label) iconValidationDecorator.createDecorationNode(new ValidationMessage() {
            @Override
            public String getText() {
                return "test";
            }

            @Override
            public Severity getSeverity() {
                return severity;
            }

            @Override
            public Control getTarget() {
                return null;
            }
        });

        assertEquals(node.getGraphic().toString(), expectedGraphic);
    }
}
