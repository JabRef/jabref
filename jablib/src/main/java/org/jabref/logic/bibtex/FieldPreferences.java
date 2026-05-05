package org.jabref.logic.bibtex;

import java.util.Collection;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class FieldPreferences {
    private static final String DEFAULT_RESOLVABLE_FIELDS = "author;booktitle;editor;editora;editorb;editorc;institution;issuetitle;journal;journalsubtitle;journaltitle;mainsubtitle;month;monthfiled;publisher;shortauthor;shorteditor;subtitle;titleaddon";
    private static final String DEFAULT_NON_WRAPPABLE_FIELDS = "pdf;ps;url;doi;file;isbn;issn";

    private final BooleanProperty resolveStrings = new SimpleBooleanProperty();
    private final ObservableList<Field> resolvableFields;
    private final ObservableList<Field> nonWrappableFields;

    /// @param resolveStrings true - The character {@link FieldWriter#BIBTEX_STRING_START_END_SYMBOL} should be interpreted as indicator of BibTeX strings
    public FieldPreferences(boolean resolveStrings,
                            List<Field> resolvableFields,
                            List<Field> nonWrappableFields) {
        this.resolveStrings.set(resolveStrings);
        this.resolvableFields = FXCollections.observableArrayList(resolvableFields);
        this.nonWrappableFields = FXCollections.observableArrayList(nonWrappableFields);
    }

    private FieldPreferences() {
        this(
                // Default resolve string setting
                true,
                // Default resolvable field
                List.copyOf(FieldFactory.parseFieldList(DEFAULT_RESOLVABLE_FIELDS)),
                // Default non wrappable field
                List.copyOf(FieldFactory.parseFieldList(DEFAULT_NON_WRAPPABLE_FIELDS))
        );
    }

    public static FieldPreferences getDefault() {
        return new FieldPreferences();
    }

    public void setAll(FieldPreferences preferences) {
        setResolveStrings(preferences.shouldResolveStrings());
        setResolvableFields(preferences.getResolvableFields());
        setNonWrappableFields(preferences.getNonWrappableFields());
    }

    public boolean shouldResolveStrings() {
        return resolveStrings.get();
    }

    public BooleanProperty resolveStringsProperty() {
        return resolveStrings;
    }

    public void setResolveStrings(boolean resolveStrings) {
        this.resolveStrings.set(resolveStrings);
    }

    public ObservableList<Field> getResolvableFields() {
        return resolvableFields;
    }

    public void setResolvableFields(Collection<Field> list) {
        resolvableFields.clear();
        resolvableFields.addAll(list);
    }

    public ObservableList<Field> getNonWrappableFields() {
        return nonWrappableFields;
    }

    public void setNonWrappableFields(Collection<Field> list) {
        nonWrappableFields.clear();
        nonWrappableFields.addAll(list);
    }
}
