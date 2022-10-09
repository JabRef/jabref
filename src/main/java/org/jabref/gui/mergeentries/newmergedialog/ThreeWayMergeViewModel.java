package org.jabref.gui.mergeentries.newmergedialog;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;

public class ThreeWayMergeViewModel extends AbstractViewModel {

    private final ObjectProperty<BibEntry> leftEntry = new SimpleObjectProperty<>();
    private final ObjectProperty<BibEntry> rightEntry = new SimpleObjectProperty<>();
    private final ObjectProperty<BibEntry> mergedEntry = new SimpleObjectProperty<>();
    private final StringProperty leftHeader = new SimpleStringProperty();
    private final StringProperty rightHeader = new SimpleStringProperty();

    private final ObservableList<Field> visibleFields = FXCollections.observableArrayList();

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

        setVisibleFields(Stream.concat(
                leftEntry.getFields().stream(),
                rightEntry.getFields().stream()).collect(Collectors.toSet()));
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

    public ObservableList<Field> getVisibleFields() {
        return visibleFields;
    }

    /**
     * Convince method to determine the total number of fields in the union of the left and right fields.
     */
    public int numberOfVisibleFields() {
        return visibleFields.size();
    }

    private void setVisibleFields(Set<Field> fields) {
        visibleFields.clear();
        visibleFields.addAll(fields);
        // Don't show internal fields. See org.jabref.model.entry.field.InternalField
        visibleFields.removeIf(FieldFactory::isInternalField);

        visibleFields.sort(Comparator.comparing(Field::getName));

        // Add the entry type field as the first field to display
        visibleFields.add(0, InternalField.TYPE_HEADER);
    }
}
