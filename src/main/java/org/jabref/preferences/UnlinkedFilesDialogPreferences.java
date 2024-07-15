package org.jabref.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.externalfiles.DateRange;
import org.jabref.gui.externalfiles.ExternalFileSorter;

public class UnlinkedFilesDialogPreferences {
    private final StringProperty unlinkedFilesSelectedExtension;
    private final ObjectProperty<DateRange> unlinkedFilesSelectedDateRange;
    private final ObjectProperty<ExternalFileSorter> unlinkedFilesSelectedSort;
    private final BooleanProperty unlinkedFilesStorePreferences;

    public UnlinkedFilesDialogPreferences(String unlinkedFilesSelectedExtension,
                                          DateRange unlinkedFilesSelectedDateRange,
                                          ExternalFileSorter unlinkedFilesSelectedSort,
                                          boolean unlinkedFilesStorePreferences) {
        this.unlinkedFilesSelectedExtension = new SimpleStringProperty(unlinkedFilesSelectedExtension);
        this.unlinkedFilesSelectedDateRange = new SimpleObjectProperty<>(unlinkedFilesSelectedDateRange);
        this.unlinkedFilesSelectedSort = new SimpleObjectProperty<>(unlinkedFilesSelectedSort);
        this.unlinkedFilesStorePreferences = new SimpleBooleanProperty(unlinkedFilesStorePreferences);
    }

    public String getUnlinkedFilesSelectedExtension() {
        return unlinkedFilesSelectedExtension.get();
    }

    public StringProperty unlinkedFilesSelectedExtensionProperty() {
        return unlinkedFilesSelectedExtension;
    }

    public void setUnlinkedFilesSelectedExtension(String unlinkedFilesSelectedExtension) {
        this.unlinkedFilesSelectedExtension.set(unlinkedFilesSelectedExtension);
    }

    public DateRange getUnlinkedFilesSelectedDateRange() {
        return unlinkedFilesSelectedDateRange.get();
    }

    public ObjectProperty<DateRange> unlinkedFilesSelectedDateRangeProperty() {
        return unlinkedFilesSelectedDateRange;
    }

    public void setUnlinkedFilesSelectedDateRange(DateRange unlinkedFilesSelectedDateRange) {
        this.unlinkedFilesSelectedDateRange.set(unlinkedFilesSelectedDateRange);
    }

    public ExternalFileSorter getUnlinkedFilesSelectedSort() {
        return unlinkedFilesSelectedSort.get();
    }

    public ObjectProperty<ExternalFileSorter> unlinkedFilesSelectedSortProperty() {
        return unlinkedFilesSelectedSort;
    }

    public void setUnlinkedFilesSelectedSort(ExternalFileSorter unlinkedFilesSelectedSort) {
        this.unlinkedFilesSelectedSort.set(unlinkedFilesSelectedSort);
    }

    public boolean isUnlinkedFilesStorePreferences() {
        return unlinkedFilesStorePreferences.get();
    }

    public BooleanProperty unlinkedFilesStorePreferencesProperty() {
        return unlinkedFilesStorePreferences;
    }

    public void setUnlinkedFilesStorePreferences(boolean unlinkedFilesStorePreferences) {
        this.unlinkedFilesStorePreferences.set(unlinkedFilesStorePreferences);
    }
}
