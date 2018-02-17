package org.jabref.gui.strings;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.logic.bibtex.comparator.BibtexStringComparator;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

public class StringDialogViewModel extends AbstractViewModel {

    private final SimpleListProperty<StringViewModel> allStrings = new SimpleListProperty<>(
            FXCollections.observableArrayList());

    private final StateManager stateManager;

    public StringDialogViewModel(StateManager stateManager) {
        this.stateManager = stateManager;
        addAllStringsFromDB();
    }

    private void addAllStringsFromDB() {
        BibDatabase db = stateManager.getActiveDatabase().get().getDatabase();
        List<StringViewModel> strings = db.getStringKeySet().stream()
                .map(string -> db.getString(string))
                .sorted(new BibtexStringComparator(false))
                .map(this::convertFromBibTexString).collect(Collectors.toList());
        allStrings.addAll(strings);
    }

    public SimpleListProperty<StringViewModel> allStringsProperty() {
        return this.allStrings;
    }

    public void addNewString() {
        allStrings.add(new StringViewModel("new Label", "New Content"));
    }

    public void removeString(StringViewModel selected) {
        allStrings.remove(selected);

    }

    private StringViewModel convertFromBibTexString(BibtexString bibtexString) {
        return new StringViewModel(bibtexString.getName(), bibtexString.getContent());

    }
}
