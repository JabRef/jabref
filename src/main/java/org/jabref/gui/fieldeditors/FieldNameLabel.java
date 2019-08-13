package org.jabref.gui.fieldeditors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

import org.jabref.model.entry.field.Field;

public class FieldNameLabel extends Label {

    public FieldNameLabel(Field field) {
        super(field.getDisplayName());

        setPadding(new Insets(4, 0, 0, 0));
        setAlignment(Pos.CENTER);
        setPrefHeight(Double.POSITIVE_INFINITY);
    }
}
