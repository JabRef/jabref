package org.jabref.gui.edit;

import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.jspecify.annotations.NonNull;

public class ReplaceStringViewModel extends AbstractViewModel {
    private boolean allFieldReplace;
    private String findString;
    private String replaceString;
    private Set<Field> fields;
    private final LibraryTab panel;

    private final StringProperty findStringProperty = new SimpleStringProperty();
    private final StringProperty replaceStringProperty = new SimpleStringProperty();
    private final StringProperty fieldStringProperty = new SimpleStringProperty();
    private final BooleanProperty allFieldReplaceProperty = new SimpleBooleanProperty();
    private final BooleanProperty selectOnlyProperty = new SimpleBooleanProperty();

    public ReplaceStringViewModel(@NonNull LibraryTab libraryTab) {
        this.panel = libraryTab;
    }

    public int replace() {
        findString = findStringProperty.getValue();
        replaceString = replaceStringProperty.getValue();
        fields = FieldFactory.parseFieldList(fieldStringProperty.getValue());
        boolean selOnly = selectOnlyProperty.getValue();
        allFieldReplace = allFieldReplaceProperty.getValue();

        final NamedCompoundEdit compound = new NamedCompoundEdit(Localization.lang("Replace string"));
        int counter = 0;
        if (selOnly) {
            for (BibEntry bibEntry : this.panel.getSelectedEntries()) {
                counter += replaceItem(bibEntry, compound);
            }
        } else {
            for (BibEntry bibEntry : this.panel.getDatabase().getEntries()) {
                counter += replaceItem(bibEntry, compound);
            }
        }
        return counter;
    }

    /**
     * Does the actual operation on a Bibtex entry based on the settings specified in this same dialog. Returns the
     * number of occurrences replaced.
     */
    private int replaceItem(BibEntry entry, NamedCompoundEdit compound) {
        int counter = 0;
        if (this.allFieldReplace) {
            for (Field field : entry.getFields()) {
                counter += replaceField(entry, field, compound);
            }
        } else {
            for (Field espField : fields) {
                counter += replaceField(entry, espField, compound);
            }
        }
        return counter;
    }

    private int replaceField(BibEntry entry, Field field, NamedCompoundEdit compound) {
        if (!entry.hasField(field)) {
            return 0;
        }
        String txt = entry.getField(field).get();
        StringBuilder stringBuilder = new StringBuilder();
        int ind;
        int piv = 0;
        int counter = 0;
        int len1 = this.findString.length();
        while ((ind = txt.indexOf(this.findString, piv)) >= 0) {
            counter++;
            stringBuilder.append(txt, piv, ind); // Text leading up to s1
            stringBuilder.append(this.replaceString); // Insert s2
            piv = ind + len1;
        }
        stringBuilder.append(txt.substring(piv));
        String newStr = stringBuilder.toString();
        entry.setField(field, newStr);
        compound.addEdit(new UndoableFieldChange(entry, field, txt, newStr));
        return counter;
    }

    public BooleanProperty allFieldReplaceProperty() {
        return allFieldReplaceProperty;
    }

    public BooleanProperty selectOnlyProperty() {
        return selectOnlyProperty;
    }

    public StringProperty fieldStringProperty() {
        return fieldStringProperty;
    }

    public StringProperty findStringProperty() {
        return findStringProperty;
    }

    public StringProperty replaceStringProperty() {
        return replaceStringProperty;
    }
}
