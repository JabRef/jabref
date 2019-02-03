package org.jabref.gui.metadata;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
    private final StringProperty newStringLabelProperty = new SimpleStringProperty("");
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
        allStrings.remove(selectedItemProperty.getValue());
    }

    private BibtexStringViewModel convertFromBibTexString(BibtexString bibtexString) {
        return new BibtexStringViewModel(bibtexString.getName(), bibtexString.getContent());
    }

    public ObjectProperty<BibtexStringViewModel> seletedItemProperty() {
        return this.selectedItemProperty;
    }

    public StringProperty newStringLabelProperty() {
        return this.newStringLabelProperty;
    }

    public void save() {
        for (BibtexStringViewModel model : allStrings) {
            String label = model.getLabel().getValue();
            String content = model.getContent().getValue();

            Optional<BibtexString> bibtexString = bibDatabase.getStringByName(label);
            if (bibtexString.isPresent()) {
                if (!(bibtexString.get().getContent().equals(content))) {
                    bibtexString.get().setContent(content);
                }
            } else {
                bibDatabase.addString(new BibtexString(label, content));
            }
        }
    }

    public BooleanProperty validProperty() {
        return validProperty;
    }

    public void openHelpPage() {
        HelpAction.openHelpPage(HelpFile.STRING_EDITOR);
    }
}
