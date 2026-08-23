package org.jabref.gui.edit;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.undo.CompoundEdit;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.undo.UndoableFieldChange;

import org.jspecify.annotations.NonNull;

public class ReplaceStringViewModel extends AbstractViewModel {
    private boolean allFieldReplace;
    private String findString;
    private String replaceString;
    private Set<Field> fields;
    private final LibraryTab libraryTab;

    private final StringProperty findStringProperty = new SimpleStringProperty();
    private final StringProperty replaceStringProperty = new SimpleStringProperty();
    private final StringProperty fieldStringProperty = new SimpleStringProperty();
    private final BooleanProperty allFieldReplaceProperty = new SimpleBooleanProperty();
    private final BooleanProperty selectOnlyProperty = new SimpleBooleanProperty();

    public ReplaceStringViewModel(@NonNull LibraryTab libraryTab) {
        this.libraryTab = libraryTab;
    }

    public int replace() {
        findString = findStringProperty.getValue();
        replaceString = replaceStringProperty.getValue();
        fields = FieldFactory.parseFieldList(fieldStringProperty.getValue());
        allFieldReplace = allFieldReplaceProperty.getValue();

        List<BibEntry> entries = selectOnlyProperty.getValue()
                                 ? libraryTab.getSelectedEntries()
                                 : libraryTab.getDatabase().getEntries();
        AtomicInteger replacements = new AtomicInteger();
        libraryTab.getUndoManager().addEdit(Localization.lang("Replace string"), edit ->
                entries.forEach(entry -> replacements.addAndGet(replaceItem(entry, edit))));
        return replacements.get();
    }

    /// Does the actual operation on a Bibtex entry based on the settings specified in this same dialog. Returns the
    /// number of occurrences replaced.
    private int replaceItem(BibEntry entry, CompoundEdit recorder) {
        int counter = 0;
        if (this.allFieldReplace) {
            for (Field field : entry.getFields()) {
                counter += replaceField(entry, field, recorder);
            }
        } else {
            for (Field espField : fields) {
                counter += replaceField(entry, espField, recorder);
            }
        }
        return counter;
    }

    private int replaceField(BibEntry entry, Field field, CompoundEdit recorder) {
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
        if (counter == 0) {
            return 0;
        }

        stringBuilder.append(txt.substring(piv));
        String newStr = stringBuilder.toString();
        entry.setField(field, newStr);
        recorder.addEdit(new UndoableFieldChange(entry, field, txt, newStr));
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
