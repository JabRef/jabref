package org.jabref.gui.fieldeditors;

import javafx.geometry.Pos;
import javafx.scene.layout.Region;

import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldNameLabelTest extends JavaFxTest {

    @Test
    void labelUsesCenterLeftAlignment() {
        FieldNameLabel label = new FieldNameLabel(StandardField.AUTHOR);
        assertEquals(Pos.CENTER_LEFT, label.getAlignment());
    }

    @Test
    void labelUsesComputedPreferredHeight() {
        FieldNameLabel label = new FieldNameLabel(StandardField.TITLE);
        assertEquals(Region.USE_COMPUTED_SIZE, label.getPrefHeight());
    }

    @ParameterizedTest
    @EnumSource(value = StandardField.class, names = {"AUTHOR", "TITLE", "JOURNALTITLE", "YEAR", "ABSTRACT"})
    void labelDisplaysCorrectDisplayName(StandardField field) {
        FieldNameLabel label = new FieldNameLabel(field);
        assertEquals(FieldsUtil.getDisplayName(field), label.getText());
    }
}
