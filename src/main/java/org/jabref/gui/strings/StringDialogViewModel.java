package org.jabref.gui.strings;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.bibtex.comparator.BibtexStringComparator;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;

public class StringDialogViewModel extends AbstractViewModel {

    private final ListProperty<StringViewModel> allStrings = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<StringViewModel> selectedItemProperty = new SimpleObjectProperty<>();
    private final StringProperty newStringLabelProperty = new SimpleStringProperty("");
    private final BibDatabase db;
    private final CompositeValidator formValidator = new CompositeValidator();

    public StringDialogViewModel(BibDatabase bibDatabase) {
        this.db = bibDatabase;
        addAllStringsFromDB();

    }

    private void addAllStringsFromDB() {

        Set<StringViewModel> strings = db.getStringKeySet()
                                         .stream()
                                         .map(string -> db.getString(string))
                                         .sorted(new BibtexStringComparator(false))
                                         .map(this::convertFromBibTexString)
                                         .collect(Collectors.toSet());
        allStrings.addAll(strings);

    }

    public ListProperty<StringViewModel> allStringsProperty() {
        return this.allStrings;
    }

    public void addNewString() {
        allStrings.add(new StringViewModel(newStringLabelProperty.getValue(), "New Content"));
    }

    public void removeString() {
        allStrings.remove(selectedItemProperty.getValue());
    }

    private StringViewModel convertFromBibTexString(BibtexString bibtexString) {
        return new StringViewModel(bibtexString.getName(), bibtexString.getContent());
    }

    public ObjectProperty<StringViewModel> seletedItemProperty() {
        return this.selectedItemProperty;
    }

    public StringProperty newStringLabelProperty() {
        return this.newStringLabelProperty;
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

    public ValidationStatus formValidation() {
        return formValidator.getValidationStatus();
    }
}
