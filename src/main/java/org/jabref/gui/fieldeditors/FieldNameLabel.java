package org.jabref.gui.fieldeditors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

public class FieldNameLabel extends Label {

    public FieldNameLabel(Field field) {
        super(field.getDisplayName());

        setPadding(new Insets(4, 0, 0, 0));
        setAlignment(Pos.CENTER);
        setPrefHeight(Double.POSITIVE_INFINITY);
        setTip(field);
    }

    public void setTip(Field field) {
        Tooltip tip = new Tooltip();
        if (field.isStandardField()) {
            StandardField standardField = (StandardField) field;
            switch (standardField) {
                case AUTHOR:
                    tip.setText(Localization.lang("The name(s) of the author(s), in the format described in the LaTeX book. Remember, all names are separated with the and keyword, and not commas."));
                    break;
                case JOURNAL:
                    tip.setText(Localization.lang("Journal name. Abbrevations may be used."));
                    break;
                case TITLE:
                    tip.setText(Localization.lang("The work's title"));
                    break;
                case YEAR:
                    tip.setText(Localization.lang("The year of publication or, for an unpublished work, the year it was written. Generally it should consist of four numerals, such as 1984, although the standard styles can handle any year whose last four nonpunctuation characters are numerals, such as '(about 1984)'."));
                    break;
                default:
                    return;
            }
        } else if (field instanceof InternalField) {
            InternalField internalField = (InternalField) field;
            switch (internalField) {
                case KEY_FIELD:
                    tip.setText(Localization.lang("[First author'last name][Article year], e.g. Jones2020"));
                    break;
                default:
                    return;
            }
        }
        this.setTooltip(tip);
    }
}
