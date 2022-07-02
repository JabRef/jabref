package org.jabref.gui.mergeentries.newmergedialog;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

class ThreeWayMergeViewModel extends AbstractViewModel {

    private final ObjectProperty<BibEntry> leftEntry = new SimpleObjectProperty<>();
    private final ObjectProperty<BibEntry> rightEntry = new SimpleObjectProperty<>();
    private final ObjectProperty<BibEntry> mergedEntry = new SimpleObjectProperty<>();
    private final StringProperty leftHeader = new SimpleStringProperty();
    private final StringProperty rightHeader = new SimpleStringProperty();

    private final ObservableList<Field> allFields = FXCollections.observableArrayList();

    public ThreeWayMergeViewModel(BibEntry leftEntry, BibEntry rightEntry, String leftHeader, String rightHeader) {
        Objects.requireNonNull(leftEntry, "Left entry is required");
        Objects.requireNonNull(rightEntry, "Right entry is required");
        Objects.requireNonNull(leftHeader, "Left header entry is required");
        Objects.requireNonNull(rightHeader, "Right header is required");

        setLeftEntry(leftEntry);
        setRightEntry(rightEntry);
        setLeftHeader(leftHeader);
        setRightHeader(rightHeader);

        mergedEntry.set(new BibEntry());

        Set<Field> leftAndRightFieldsUnion = new HashSet<>(leftEntry.getFields());
        leftAndRightFieldsUnion.addAll(rightEntry.getFields());
        setAllFields(leftAndRightFieldsUnion);
    }

    public StringProperty leftHeaderProperty() {
        return leftHeader;
    }

    public String getLeftHeader() {
        return leftHeader.get();
    }

    public void setLeftHeader(String leftHeader) {
        leftHeaderProperty().set(leftHeader);
    }

    public StringProperty rightHeaderProperty() {
        return rightHeader;
    }

    public String getRightHeader() {
        return rightHeaderProperty().get();
    }

    public void setRightHeader(String rightHeader) {
        rightHeaderProperty().set(rightHeader);
    }

    public BibEntry getLeftEntry() {
        return leftEntry.get();
    }

    private void setLeftEntry(BibEntry bibEntry) {
        leftEntry.set(bibEntry);
    }

    public BibEntry getRightEntry() {
        return rightEntry.get();
    }

    private void setRightEntry(BibEntry bibEntry) {
        rightEntry.set(bibEntry);
    }

    public BibEntry getMergedEntry() {
        return mergedEntry.get();
    }

    public ObservableList<Field> allFields() {
        return allFields;
    }

    /**
     * Convince method to determine the total number of fields in the union of the left and right fields.
     */
    public int allFieldsSize() {
        return allFields.size();
    }

    private void setAllFields(Set<Field> fields) {
        allFields.clear();
        allFields.addAll(fields);
        allFields.sort(Comparator.comparing(Field::getName));
    }
}
