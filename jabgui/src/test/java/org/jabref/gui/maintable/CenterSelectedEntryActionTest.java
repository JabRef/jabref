package org.jabref.gui.maintable;

import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import org.jabref.gui.StateManager;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
class CenterSelectedEntryActionTest {

    private StateManager stateManager;
    private MainTable mainTable;
    private TableView.TableViewSelectionModel selectionModel;
    private ObservableList<BibEntry> selectedEntries;

    @BeforeEach
    void setUp() {
        stateManager = mock(StateManager.class);
        mainTable = mock(MainTable.class);
        selectionModel = mock(TableView.TableViewSelectionModel.class);
        when(mainTable.getSelectionModel()).thenReturn(selectionModel);
        when(selectionModel.getSelectedIndex()).thenReturn(0);
        selectedEntries = FXCollections.observableArrayList();
        when(stateManager.getSelectedEntries()).thenReturn(selectedEntries);
    }

    @Test
    void actionDisabledWhenNoEntriesSelected() {
        CenterSelectedEntryAction action = new CenterSelectedEntryAction(() -> Optional.of(mainTable), stateManager);
        assertFalse(action.executableProperty().getValue());
    }

    @Test
    void actionEnabledWhenEntriesSelected() {
        selectedEntries.add(new BibEntry());
        CenterSelectedEntryAction action = new CenterSelectedEntryAction(() -> Optional.of(mainTable), stateManager);
        assertTrue(action.executableProperty().getValue());
    }

    // [utest->req~maintable.center-selected~1]
    @Test
    void executeCallsCenterSelectedEntryOnTable() {
        CenterSelectedEntryAction action = new CenterSelectedEntryAction(() -> Optional.of(mainTable), stateManager);
        action.execute();
        verify(mainTable).centerSelectedEntry();
    }

    @Test
    void executeSelectsEntryInTableIfTableSelectionIsEmpty() {
        BibEntry entry = new BibEntry();
        selectedEntries.add(entry);
        when(selectionModel.getSelectedIndex()).thenReturn(-1);

        CenterSelectedEntryAction action = new CenterSelectedEntryAction(() -> Optional.of(mainTable), stateManager);
        action.execute();

        verify(mainTable).clearAndSelect(entry);
        verify(mainTable).centerSelectedEntry();
    }

    @Test
    void executeDoesNothingWhenTableNotPresent() {
        CenterSelectedEntryAction action = new CenterSelectedEntryAction(Optional::empty, stateManager);
        action.execute();
        verifyNoInteractions(mainTable);
    }
}
