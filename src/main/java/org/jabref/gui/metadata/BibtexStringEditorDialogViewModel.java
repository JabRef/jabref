package org.jabref.gui.metadata;

import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.bibtex.comparator.BibtexStringComparator;
import org.jabref.logic.help.HelpFile;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

import com.tobiasdiez.easybind.EasyBind;

public class BibtexStringEditorDialogViewModel extends AbstractViewModel {
    private static final String NEW_STRING_LABEL = "NewString"; // must not contain spaces

    private final ListProperty<BibtexStringEditorItemModel> stringsListProperty =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BibDatabase bibDatabase;
    private final BooleanProperty validProperty = new SimpleBooleanProperty();

    public BibtexStringEditorDialogViewModel(BibDatabase bibDatabase) {
        this.bibDatabase = bibDatabase;
        addAllStringsFromDB();

        ObservableList<ObservableValue<Boolean>> allValidProperty =
                EasyBind.map(stringsListProperty(), BibtexStringEditorItemModel::combinedValidationValidProperty);
        validProperty.bind(EasyBind.combine(allValidProperty, stream -> stream.allMatch(valid -> valid)));
    }

    private void addAllStringsFromDB() {
        stringsListProperty.addAll(bibDatabase.getStringValues().stream()
                                              .sorted(new BibtexStringComparator(false))
                                              .map(this::convertFromBibTexString)
                                              .collect(Collectors.toSet()));
    }

    public void addNewString() {
        BibtexStringEditorItemModel newItem;
        if (labelAlreadyExists(NEW_STRING_LABEL).isPresent()) {
            int i = 1;
            while (labelAlreadyExists(NEW_STRING_LABEL + i).isPresent()) {
                i++;
            }
            newItem = new BibtexStringEditorItemModel(NEW_STRING_LABEL + i, "");
        } else {
            newItem = new BibtexStringEditorItemModel(NEW_STRING_LABEL, "");
        }

        stringsListProperty.add(newItem);
    }

    public void removeString(BibtexStringEditorItemModel item) {
        stringsListProperty.remove(item);
    }

    private BibtexStringEditorItemModel convertFromBibTexString(BibtexString bibtexString) {
        return new BibtexStringEditorItemModel(bibtexString.getName(), bibtexString.getContent());
    }

    public void save() {
        bibDatabase.setStrings(stringsListProperty.stream()
                                                  .map(this::fromBibtexStringViewModel)
                                                  .collect(Collectors.toList()));
    }

    private BibtexString fromBibtexStringViewModel(BibtexStringEditorItemModel viewModel) {
        String label = viewModel.labelProperty().getValue();
        String content = viewModel.contentProperty().getValue();
        return new BibtexString(label, content);
    }

    public Optional<BibtexStringEditorItemModel> labelAlreadyExists(String label) {
        return stringsListProperty.stream()
                                  .filter(item -> item.labelProperty().getValue().equals(label))
                                  .findFirst();
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
}
