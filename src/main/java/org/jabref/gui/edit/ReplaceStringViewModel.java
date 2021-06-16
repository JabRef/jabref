package org.jabref.gui.edit;

import java.util.Objects;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class ReplaceStringViewModel extends AbstractViewModel {
    private boolean allFieldReplace;
    private String findString;
    private String replaceString;
    private Set<Field> fields;
    private LibraryTab panel;

    private StringProperty findStringProperty = new SimpleStringProperty();
    private StringProperty replaceStringProperty = new SimpleStringProperty();
    private StringProperty fieldStringProperty = new SimpleStringProperty();
    private BooleanProperty allFieldReplaceProperty = new SimpleBooleanProperty();
    private BooleanProperty selectOnlyProperty = new SimpleBooleanProperty();

    public ReplaceStringViewModel(LibraryTab libraryTab) {
        Objects.requireNonNull(libraryTab);
        this.panel = libraryTab;
    }

    public int replace() {
        findString = findStringProperty.getValue();
        replaceString = replaceStringProperty.getValue();
        fields = FieldFactory.parseFieldList(fieldStringProperty.getValue());
        boolean selOnly = selectOnlyProperty.getValue();
        allFieldReplace = allFieldReplaceProperty.getValue();

        final NamedCompound compound = new NamedCompound(Localization.lang("Replace string"));
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
    private int replaceItem(BibEntry entry, NamedCompound compound) {
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

    private int replaceField(BibEntry entry, Field field, NamedCompound compound) {
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
