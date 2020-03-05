package org.jabref.gui.fieldeditors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class FieldNameLabel extends Label {

    public FieldNameLabel(Field field, String name) {
        super(field.getDisplayName());

        setPadding(new Insets(4, 0, 0, 0));
        setAlignment(Pos.CENTER);
        setPrefHeight(Double.POSITIVE_INFINITY);
        setTip(name);
    }

    public void setTip(String name) {
        Tooltip tip = new Tooltip();
        if (StandardField.AUTHOR.getName().equals(name)) {
            tip.setText(Localization.lang("Multiple authors separated with 'and', e.g. author1 and author2"));
        } else if (StandardField.BIBTEXKEY.getName().equals(name)) {
            tip.setText(Localization.lang("[First author'last name][Article year] e.g. Jones2020"));
        } else if (StandardField.JOURNAL.getName().equals(name)) {
            tip.setText(Localization.lang("The name of the journal"));
        } else if (StandardField.TITLE.getName().equals(name)) {
            tip.setText(Localization.lang("The title of the article"));
        } else if (StandardField.YEAR.getName().equals(name)) {
            tip.setText(Localization.lang("The year of publication, e.g. 2005"));
        } else {
            return;
        }
        this.setTooltip(tip);
    }
}
