package org.jabref.gui.metadata;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.bibtex.comparator.BibtexStringComparator;
import org.jabref.logic.help.HelpFile;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

import org.fxmisc.easybind.EasyBind;

public class BibtexStringEditorDialogViewModel extends AbstractViewModel {

    private final ListProperty<BibtexStringViewModel> allStrings = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<BibtexStringViewModel> selectedItemProperty = new SimpleObjectProperty<>();
    private final BibDatabase bibDatabase;
    private final BooleanProperty validProperty = new SimpleBooleanProperty();

    public BibtexStringEditorDialogViewModel(BibDatabase bibDatabase) {
        this.bibDatabase = bibDatabase;
        addAllStringsFromDB();

        ObservableList<ObservableValue<Boolean>> allValidProperty = EasyBind.map(allStringsProperty(), BibtexStringViewModel::combinedValidationValidProperty);
        validProperty.bind(EasyBind.combine(allValidProperty, stream -> stream.allMatch(valid -> valid)));
    }

    private void addAllStringsFromDB() {

        Set<BibtexStringViewModel> strings = bibDatabase.getStringValues()
                                                        .stream()
                                                        .sorted(new BibtexStringComparator(false))
                                                        .map(this::convertFromBibTexString)
                                                        .collect(Collectors.toSet());
        allStrings.addAll(strings);
    }

    public ListProperty<BibtexStringViewModel> allStringsProperty() {
        return this.allStrings;
    }

    public void addNewString() {
        allStrings.add(new BibtexStringViewModel("", ""));
    }

    public void removeString() {
        BibtexStringViewModel toBeRemoved = selectedItemProperty.getValue();
        allStrings.remove(toBeRemoved);
    }

    private BibtexStringViewModel convertFromBibTexString(BibtexString bibtexString) {
        return new BibtexStringViewModel(bibtexString.getName(), bibtexString.getContent());
    }

    public ObjectProperty<BibtexStringViewModel> seletedItemProperty() {
        return this.selectedItemProperty;
    }

    public void save() {
        List<BibtexString> stringsToAdd = allStrings.stream().map(this::fromBibtexStringViewModel).collect(Collectors.toList());
        bibDatabase.setStrings(stringsToAdd);
    }

    private BibtexString fromBibtexStringViewModel(BibtexStringViewModel viewModel) {
        String label = viewModel.labelProperty().getValue();
        String content = viewModel.contentProperty().getValue();
        return new BibtexString(label, content);
    }

    public BooleanProperty validProperty() {
        return validProperty;
    }

    public void openHelpPage() {
        HelpAction.openHelpPage(HelpFile.STRING_EDITOR);
    }
}
