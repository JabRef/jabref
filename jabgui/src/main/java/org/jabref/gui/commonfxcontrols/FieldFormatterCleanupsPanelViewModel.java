package org.jabref.gui.commonfxcontrols;

import java.util.Comparator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.SelectionModel;

import org.jabref.gui.StateManager;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.formatter.Formatters;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldTextMapper;

public class FieldFormatterCleanupsPanelViewModel {

    private final BooleanProperty cleanupsDisableProperty = new SimpleBooleanProperty();
    private final ListProperty<FieldFormatterCleanup> cleanupsListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<SelectionModel<FieldFormatterCleanup>> selectedCleanupProperty = new SimpleObjectProperty<>(new NoSelectionModel<>());
    private final ListProperty<Field> availableFieldsProperty = new SimpleListProperty<>(new SortedList<>(FXCollections.observableArrayList(FieldFactory.getCommonFields()), Comparator.comparing(FieldTextMapper::getDisplayName)));
    private final ObjectProperty<Field> selectedFieldProperty = new SimpleObjectProperty<>();
    private final ListProperty<Formatter> availableFormattersProperty = new SimpleListProperty<>(new SortedList<>(FXCollections.observableArrayList(Formatters.getAll()), Comparator.comparing(Formatter::getName)));
    private final ObjectProperty<Formatter> selectedFormatterProperty = new SimpleObjectProperty<>();

    private final StateManager stateManager;

    public FieldFormatterCleanupsPanelViewModel(StateManager stateManager) {
        this.stateManager = stateManager;
    }

    public void resetToRecommended() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            if (databaseContext.isBiblatexMode()) {
                cleanupsListProperty.setAll(FieldFormatterCleanupActions.RECOMMEND_BIBLATEX_ACTIONS);
            } else {
                cleanupsListProperty.setAll(FieldFormatterCleanupActions.RECOMMEND_BIBTEX_ACTIONS);
            }
        });
    }

    public void clearAll() {
        cleanupsListProperty.clear();
    }

    public void addCleanup() {
        if (selectedFieldProperty.getValue() == null || selectedFormatterProperty.getValue() == null) {
            return;
        }

        FieldFormatterCleanup cleanup = new FieldFormatterCleanup(
                selectedFieldProperty.getValue(),
                selectedFormatterProperty.getValue());

        if (cleanupsListProperty.stream().noneMatch(item -> item.equals(cleanup))) {
            cleanupsListProperty.add(cleanup);
        }
    }

    public void removeCleanup(FieldFormatterCleanup cleanup) {
        cleanupsListProperty.remove(cleanup);
    }

    public BooleanProperty cleanupsDisableProperty() {
        return cleanupsDisableProperty;
    }

    public ListProperty<FieldFormatterCleanup> cleanupsListProperty() {
        return cleanupsListProperty;
    }

    public ObjectProperty<SelectionModel<FieldFormatterCleanup>> selectedCleanupProperty() {
        return selectedCleanupProperty;
    }

    public ListProperty<Field> availableFieldsProperty() {
        return availableFieldsProperty;
    }

    public ObjectProperty<Field> selectedFieldProperty() {
        return selectedFieldProperty;
    }

    public ListProperty<Formatter> availableFormattersProperty() {
        return availableFormattersProperty;
    }

    public ObjectProperty<Formatter> selectedFormatterProperty() {
        return selectedFormatterProperty;
    }
}
