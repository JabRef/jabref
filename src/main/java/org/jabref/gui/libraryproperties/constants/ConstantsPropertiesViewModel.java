package org.jabref.gui.libraryproperties.constants;

import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.help.HelpAction;
import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.logic.bibtex.comparator.BibtexStringComparator;
import org.jabref.logic.help.HelpFile;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;

import com.tobiasdiez.easybind.EasyBind;

public class ConstantsPropertiesViewModel implements PropertiesTabViewModel {

    private static final String NEW_STRING_LABEL = "NewString"; // must not contain spaces

    private final ListProperty<ConstantsItemModel> stringsListProperty =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BooleanProperty validProperty = new SimpleBooleanProperty();

    private final BibDatabaseContext databaseContext;

    public ConstantsPropertiesViewModel(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ObservableList<ObservableValue<Boolean>> allValidProperty =
                EasyBind.map(stringsListProperty, ConstantsItemModel::combinedValidationValidProperty);
        validProperty.bind(EasyBind.combine(allValidProperty, stream -> stream.allMatch(valid -> valid)));
    }

    @Override
    public void setValues() {
        stringsListProperty.addAll(databaseContext.getDatabase().getStringValues().stream()
                                                  .sorted(new BibtexStringComparator(false))
                                                  .map(this::convertFromBibTexString)
                                                  .collect(Collectors.toSet()));
    }

    public void addNewString() {
        ConstantsItemModel newItem;
        if (labelAlreadyExists(NEW_STRING_LABEL).isPresent()) {
            int i = 1;
            while (labelAlreadyExists(NEW_STRING_LABEL + i).isPresent()) {
                i++;
            }
            newItem = new ConstantsItemModel(NEW_STRING_LABEL + i, "");
        } else {
            newItem = new ConstantsItemModel(NEW_STRING_LABEL, "");
        }

        stringsListProperty.add(newItem);
    }

    public void removeString(ConstantsItemModel item) {
        stringsListProperty.remove(item);
    }

    private ConstantsItemModel convertFromBibTexString(BibtexString bibtexString) {
        return new ConstantsItemModel(bibtexString.getName(), bibtexString.getContent());
    }

    @Override
    public void storeSettings() {
        databaseContext.getDatabase().setStrings(stringsListProperty.stream()
                                                                    .map(this::fromBibtexStringViewModel)
                                                                    .collect(Collectors.toList()));
    }

    private BibtexString fromBibtexStringViewModel(ConstantsItemModel viewModel) {
        String label = viewModel.labelProperty().getValue();
        String content = viewModel.contentProperty().getValue();
        return new BibtexString(label, content);
    }

    public Optional<ConstantsItemModel> labelAlreadyExists(String label) {
        return stringsListProperty.stream()
                                  .filter(item -> item.labelProperty().getValue().equals(label))
                                  .findFirst();
    }

    public void openHelpPage() {
        HelpAction.openHelpPage(HelpFile.STRING_EDITOR);
    }

    public ListProperty<ConstantsItemModel> stringsListProperty() {
        return stringsListProperty;
    }

    public BooleanProperty validProperty() {
        return validProperty;
    }
}
