package org.jabref.gui.commonfxcontrols;

import java.util.Collections;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.model.entry.field.Field;
import org.jabref.model.metadata.SaveOrderConfig;

public class SaveOrderConfigPanelViewModel {

    private final BooleanProperty saveInOriginalProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInTableOrderProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInSpecifiedOrderProperty = new SimpleBooleanProperty();

    private final ListProperty<Field> sortableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<SortCriterionViewModel> selectedSortCriteriaProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

    public SaveOrderConfigPanelViewModel() {
    }

    public void addCriterion() {
        selectedSortCriteriaProperty.add(new SortCriterionViewModel(new SaveOrderConfig.SortCriterion()));
    }

    public void removeCriterion(SortCriterionViewModel sortCriterionViewModel) {
        selectedSortCriteriaProperty.remove(sortCriterionViewModel);
    }

    public void moveCriterionUp(SortCriterionViewModel sortCriterionViewModel) {
        if (selectedSortCriteriaProperty.contains(sortCriterionViewModel)) {
            int index = selectedSortCriteriaProperty.indexOf(sortCriterionViewModel);
            if (index > 0) {
                Collections.swap(selectedSortCriteriaProperty, index - 1, index);
            }
        }
    }

    public void moveCriterionDown(SortCriterionViewModel sortCriterionViewModel) {
        if (selectedSortCriteriaProperty.contains(sortCriterionViewModel)) {
            int index = selectedSortCriteriaProperty.indexOf(sortCriterionViewModel);
            if (index >= 0 && index < selectedSortCriteriaProperty.size() - 1) {
                Collections.swap(selectedSortCriteriaProperty, index + 1, index);
            }
        }
    }

    public BooleanProperty saveInOriginalProperty() {
        return saveInOriginalProperty;
    }

    public BooleanProperty saveInTableOrderProperty() {
        return saveInTableOrderProperty;
    }

    public BooleanProperty saveInSpecifiedOrderProperty() {
        return saveInSpecifiedOrderProperty;
    }

    public ListProperty<Field> sortableFieldsProperty() {
        return sortableFieldsProperty;
    }

    public ListProperty<SortCriterionViewModel> sortCriteriaProperty() {
        return selectedSortCriteriaProperty;
    }
}
