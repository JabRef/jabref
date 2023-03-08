package org.jabref.gui.search;

import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.logic.search.SearchHistoryItem;

public class HistoryDialogViewModel extends AbstractViewModel {
    private final StateManager stateManager;

    public HistoryDialogViewModel(StateManager stateManager) {
        this.stateManager = stateManager;
    }

    public ObservableList<SearchHistoryItem> getHistory() {
        return this.stateManager.getSearchHistory().getHistory();
    }

    public void remove(String searchTerm) {
        this.stateManager.getSearchHistory().remove(searchTerm);
    }
}
