package org.jabref.gui.cleanup;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;

public class CleanupSingleFieldViewModel {
    public final BooleanProperty cleanupsEnabled = new SimpleBooleanProperty(true);
    public final ListProperty<FieldFormatterCleanup> cleanups = new SimpleListProperty<>(FXCollections.observableArrayList());

    public CleanupSingleFieldViewModel(FieldFormatterCleanupActions initialCleanups) {
        cleanupsEnabled.set(initialCleanups.isEnabled());
        cleanups.setAll(initialCleanups.getConfiguredActions());
    }

    public FieldFormatterCleanupActions getSelectedFormatters() {
        return new FieldFormatterCleanupActions(cleanupsEnabled.get(), cleanups.get());
    }
}
