package org.jabref.gui.metadata;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
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
import org.jabref.model.strings.StringUtil;

import org.fxmisc.easybind.EasyBind;

public class BibtexStringEditorDialogViewModel extends AbstractViewModel {

    private final ListProperty<BibtexStringEditorItemModel> stringsListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty addLabelProperty = new SimpleStringProperty();
    private final StringProperty addContentProperty = new SimpleStringProperty();

    private final BibDatabase bibDatabase;
    private final BooleanProperty validProperty = new SimpleBooleanProperty();

    public BibtexStringEditorDialogViewModel(BibDatabase bibDatabase) {
        this.bibDatabase = bibDatabase;
        addAllStringsFromDB();

        ObservableList<ObservableValue<Boolean>> allValidProperty = EasyBind.map(stringsListProperty(), BibtexStringEditorItemModel::combinedValidationValidProperty);
        validProperty.bind(EasyBind.combine(allValidProperty, stream -> stream.allMatch(valid -> valid)));
    }

    private void addAllStringsFromDB() {
        Set<BibtexStringEditorItemModel> strings = bibDatabase.getStringValues().stream()
                .sorted(new BibtexStringComparator(false))
                .map(this::convertFromBibTexString)
                .collect(Collectors.toSet());
        stringsListProperty.addAll(strings);
    }

    public boolean addNewString() {
        if (!StringUtil.isNullOrEmpty(addLabelProperty.getValue())
                && !labelAlreadyExists(addLabelProperty.getValue())) {
            stringsListProperty.add(new BibtexStringEditorItemModel(addLabelProperty.getValue(), addContentProperty.getValue()));
            addLabelProperty.setValue("");
            addContentProperty.setValue("");
            return true;
        } else {
            return false;
        }
    }

    public void removeString(BibtexStringEditorItemModel item) {
        stringsListProperty.remove(item);
    }

    private BibtexStringEditorItemModel convertFromBibTexString(BibtexString bibtexString) {
        return new BibtexStringEditorItemModel(bibtexString.getName(), bibtexString.getContent());
    }

    public void save() {
        List<BibtexString> stringsToAdd = stringsListProperty.stream().map(this::fromBibtexStringViewModel).collect(Collectors.toList());
        bibDatabase.setStrings(stringsToAdd);
    }

    private BibtexString fromBibtexStringViewModel(BibtexStringEditorItemModel viewModel) {
        String label = viewModel.labelProperty().getValue();
        String content = viewModel.contentProperty().getValue();
        return new BibtexString(label, content);
    }

    public boolean labelAlreadyExists(String label) {
        return stringsListProperty.stream().anyMatch(item -> item.labelProperty().getValue().equals(label));
    }

    public void openHelpPage() {
        HelpAction.openHelpPage(HelpFile.STRING_EDITOR);
    }

    public ListProperty<BibtexStringEditorItemModel> stringsListProperty() {
        return stringsListProperty;
    }

    public BooleanProperty validProperty() {
        return validProperty;
    }

    public StringProperty addLabelProperty() {
        return addLabelProperty;
    }

    public StringProperty addContentProperty() {
        return addContentProperty;
    }
}
