package org.jabref.gui.fieldeditors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

import javafx.scene.control.Tooltip;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class FieldNameLabel extends Label {
    private Tooltip tip;
    private static final String AUTHOR = "author";
    private static final String BIBTEXKEY = "bibtexkey";
    private static final String JOURNAL = "journal";
    private static final String TITLE = "title";
    private static final String YEAR = "year";

    private static final String AUTHOR_TIP = "Multiple authors separated with 'and', e.g. author1 and author2";
    private static final String BIBTEXKEY_TIP = "[First author'last name][Article year] e.g. Jones2020";
    private static final String JOURNAL_TIP = "The name of the journal";
    private static final String TITLE_TIP = "The title of the article";
    private static final String YEAR_TIP = "The year of publication, e.g. 2005";

    public FieldNameLabel(Field field, String name) {
        super(field.getDisplayName());

        setPadding(new Insets(4, 0, 0, 0));
        setAlignment(Pos.CENTER);
        setPrefHeight(Double.POSITIVE_INFINITY);
        setTip(name);
    }

    public void setTip(String name){
        tip = new Tooltip();

        switch (name){
            case AUTHOR:
                {
                tip.setText(AUTHOR_TIP);
                break;
            }
            case BIBTEXKEY:
            {
                tip.setText(BIBTEXKEY_TIP);
                break;
            }
            case JOURNAL:
            {
                tip.setText(JOURNAL_TIP);
                break;
            }
            case TITLE:
            {
                tip.setText(TITLE_TIP);
                break;
            }
            case YEAR:
            {
                tip.setText(YEAR_TIP);
                break;
            }

            default:{
                return;
            }
        }
        this.setTooltip(tip);
    }
}
