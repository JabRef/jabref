package org.jabref.gui.commonfxcontrols;

import java.util.EnumMap;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.cleanup.BooktitleCleanups;
import org.jabref.logic.util.LocationDetector;
import org.jabref.model.cleanup.BooktitleCleanupAction;
import org.jabref.model.cleanup.BooktitleCleanupField;

public class BooktitleCleanupPanelViewModel {

    private final BooleanProperty cleanupsDisableProperty = new SimpleBooleanProperty();
    /*
     * Map Property mapping each cleanup field to its corresponding cleanup action.
     * Each field corresponds to a ToggleGroup in the Cleanup Panel with each Radio Button representing
     * an individual cleanup action.
     */
    private final MapProperty<BooktitleCleanupField, BooktitleCleanupAction> selectedActions =
            new SimpleMapProperty<>(FXCollections.observableMap(new EnumMap<>(BooktitleCleanupField.class)));

    private final LocationDetector locationDetector;

    public BooktitleCleanupPanelViewModel(LocationDetector locationDetector) {
        this.locationDetector = locationDetector;
    }

    public BooktitleCleanups createCleanup() {
        return new BooktitleCleanups(
                getSelectedAction(BooktitleCleanupField.YEAR),
                getSelectedAction(BooktitleCleanupField.MONTH),
                getSelectedAction(BooktitleCleanupField.PAGE_RANGE),
                getSelectedAction(BooktitleCleanupField.LOCATION),
                locationDetector
        );
    }

    public MapProperty<BooktitleCleanupField, BooktitleCleanupAction> selectedActionsProperty() {
        return selectedActions;
    }

    public void setSelectedAction(BooktitleCleanupField field, BooktitleCleanupAction action) {
        selectedActions.put(field, action);
    }

    public BooktitleCleanupAction getSelectedAction(BooktitleCleanupField field) {
        return selectedActions.get(field);
    }

    public BooleanProperty cleanupsDisableProperty() {
        return cleanupsDisableProperty;
    }
}
