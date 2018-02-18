package org.jabref.gui.strings;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.logic.bibtex.comparator.BibtexStringComparator;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

public class StringDialogViewModel extends AbstractViewModel {

    private final ListProperty<StringViewModel> allStrings = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BibDatabase db;

    public StringDialogViewModel(StateManager stateManager) {
        this.db = stateManager.getActiveDatabase().get().getDatabase();
        addAllStringsFromDB();

    }

    private void addAllStringsFromDB() {

        Set<StringViewModel> strings = db.getStringKeySet().stream()
                .map(string -> db.getString(string))
                .sorted(new BibtexStringComparator(false))
                .map(this::convertFromBibTexString).collect(Collectors.toSet());
        allStrings.addAll(strings);
    }

    public ListProperty<StringViewModel> allStringsProperty() {
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

    public void validateInput(StringViewModel newValue) {
        if (newValue != null) {
            String label = newValue.getLabel().getValue();
            String content = newValue.getContent().getValue();

            //TODO: Validation
        }

    }

    public void save() {
        for (StringViewModel model : allStrings) {
            String label = model.getLabel().getValue();
            String content = model.getContent().getValue();

            Optional<BibtexString> bibtexString = db.getStringByName(label);
            if (bibtexString.isPresent()) {
                if (!(bibtexString.get().getContent().equals(content))) {
                    bibtexString.get().setContent(content);
                }
            } else {
                db.addString(new BibtexString(label, content));
            }
        }

    }
}
