package org.jabref.gui.commonfxcontrols;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.SelectionModel;

import org.jabref.gui.Globals;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.Formatter;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class FieldFormatterCleanupsPanelViewModel {

    private final BooleanProperty cleanupsDisableProperty = new SimpleBooleanProperty();
    private final ListProperty<FieldFormatterCleanup> cleanupsListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<SelectionModel<FieldFormatterCleanup>> selectedCleanupProperty = new SimpleObjectProperty<>(new NoSelectionModel<>());
    private final ListProperty<Field> availableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList(FieldFactory.getCommonFields()));
    private final ObjectProperty<Field> selectedFieldProperty = new SimpleObjectProperty<>();
    private final ListProperty<Formatter> availableFormattersProperty = new SimpleListProperty<>(FXCollections.observableArrayList(Cleanups.getBuiltInFormatters()));
    private final ObjectProperty<Formatter> selectedFormatterProperty = new SimpleObjectProperty<>();

    public FieldFormatterCleanupsPanelViewModel() {
        availableFieldsProperty.sort((field1, field2) -> {
            if (field1.getDisplayName().equals(field2.getDisplayName())) {
                return 0;
            }
            return field1.getDisplayName().compareTo(field2.getDisplayName()) > 0 ? 1 : -1;
        });
    }

    public void resetToRecommended() {
        Globals.stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            if (databaseContext.isBiblatexMode()) {
                cleanupsListProperty.setAll(Cleanups.RECOMMEND_BIBLATEX_ACTIONS.getConfiguredActions());
            } else {
                cleanupsListProperty.setAll(Cleanups.RECOMMEND_BIBTEX_ACTIONS.getConfiguredActions());
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
