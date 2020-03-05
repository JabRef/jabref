package org.jabref.gui.fieldeditors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class FieldNameLabel extends Label {
    private Tooltip tip;

    public FieldNameLabel(Field field, String name) {
        super(field.getDisplayName());

        setPadding(new Insets(4, 0, 0, 0));
        setAlignment(Pos.CENTER);
        setPrefHeight(Double.POSITIVE_INFINITY);
        setTip(name);
    }

    public void setTip(String name){
        tip = new Tooltip();
        if (StandardField.AUTHOR.getName().equals(name)) {
            tip.setText(Localization.lang("AUTHOR_TIP"));
        } else if (StandardField.BIBTEXKEY.getName().equals(name)) {
            tip.setText(Localization.lang("BIBTEXKEY_TIP"));
        } else if (StandardField.JOURNAL.getName().equals(name)) {
            tip.setText(Localization.lang("JOURNAL_TIP"));
        } else if (StandardField.TITLE.getName().equals(name)) {
            tip.setText(Localization.lang("TITLE_TIP"));
        } else if (StandardField.YEAR.getName().equals(name)) {
            tip.setText(Localization.lang("YEAR_TIP"));
        } else {
            return;
        }
        this.setTooltip(tip);
    }
}
