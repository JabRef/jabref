package org.jabref.gui.maintable;

import javafx.beans.property.SimpleObjectProperty;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class BibEntryTableViewModelTest {

    @Test
    void keepsStateSetBeforePropertiesAreObserved() {
        BibEntryTableViewModel viewModel = new BibEntryTableViewModel(new BibEntry(), new BibDatabaseContext(), new SimpleObjectProperty<>());

        viewModel.setHasFullTextResults(true);
        viewModel.setMatchedBySearch(false);
        viewModel.setVisibleBySearch(false);
        viewModel.setMatchedByGroup(false);
        viewModel.setVisibleByGroup(false);

        assertTrue(viewModel.hasFullTextResultsProperty().get());
        assertFalse(viewModel.isMatchedBySearch().get());
        assertFalse(viewModel.isVisibleBySearch().get());
        assertFalse(viewModel.isMatchedByGroup().get());
        assertFalse(viewModel.isVisibleByGroup().get());
        assertFalse(viewModel.isVisible());
    }
}
