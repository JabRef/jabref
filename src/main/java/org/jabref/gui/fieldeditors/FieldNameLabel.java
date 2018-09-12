package org.jabref.gui.fieldeditors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

import org.jabref.model.entry.FieldName;

public class FieldNameLabel extends Label {

    public FieldNameLabel(String fieldName) {
        super(FieldName.getDisplayName(fieldName));

        setPadding(new Insets(4, 0, 0, 0));
        setAlignment(Pos.CENTER);
        setPrefHeight(Double.POSITIVE_INFINITY);
    }
}
