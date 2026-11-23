package org.jabref.gui.maintable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;

import org.jspecify.annotations.NullMarked;

/// Passes all changes of the source on and additionally reports a row as updated once its displayed values changed.
///
/// The source already reports an update when an entry changes, but before the cached values of the row are recomputed.
/// A sorted list would then move the row using its old value.
/// Reporting the row again after its values changed lets the sorted list move only this row to its correct position.
@NullMarked
class RowValueUpdatesList extends TransformationList<BibEntryTableViewModel, BibEntryTableViewModel> {

    private final SequencedSet<BibEntryTableViewModel> changedRows = new LinkedHashSet<>();
    private final InvalidationListener rowValuesListener = this::onRowValuesChanged;

    RowValueUpdatesList(ObservableList<BibEntryTableViewModel> source) {
        super(source);
        source.forEach(row -> row.valuesRevisionProperty().addListener(rowValuesListener));
    }

    @Override
    protected void sourceChanged(ListChangeListener.Change<? extends BibEntryTableViewModel> change) {
        while (change.next()) {
            change.getRemoved().forEach(row -> row.valuesRevisionProperty().removeListener(rowValuesListener));
            change.getAddedSubList().forEach(row -> row.valuesRevisionProperty().addListener(rowValuesListener));
        }
        change.reset();
        fireChange(change);
    }

    private void onRowValuesChanged(Observable valuesRevision) {
        BibEntryTableViewModel row = (BibEntryTableViewModel) ((ReadOnlyProperty<?>) valuesRevision).getBean();
        boolean reportAlreadyScheduled;
        synchronized (changedRows) {
            reportAlreadyScheduled = !changedRows.isEmpty();
            changedRows.add(row);
        }
        // Several values of a row change together, possibly outside the JavaFX thread, so report the rows once later
        if (!reportAlreadyScheduled) {
            Platform.runLater(this::reportChangedRows);
        }
    }

    private void reportChangedRows() {
        List<BibEntryTableViewModel> rows;
        synchronized (changedRows) {
            rows = List.copyOf(changedRows);
            changedRows.clear();
        }
        beginChange();
        for (BibEntryTableViewModel row : rows) {
            int index = getSource().indexOf(row);
            if (index >= 0) {
                nextUpdate(index);
            }
        }
        endChange();
    }

    @Override
    public int getSourceIndex(int index) {
        return index;
    }

    @Override
    public int getViewIndex(int index) {
        return index;
    }

    @Override
    public BibEntryTableViewModel get(int index) {
        return getSource().get(index);
    }

    @Override
    public int size() {
        return getSource().size();
    }
}
