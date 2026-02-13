package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.commonfxcontrols.MultiFieldsCleanupPanel;
import org.jabref.logic.cleanup.CleanupPreferences;

public class CleanupMultiFieldViewModel {
    public final EnumSet<CleanupPreferences.CleanupStep> allJobs;
    public final SetProperty<CleanupPreferences.CleanupStep> activeJobs = new SimpleSetProperty<>(FXCollections.observableSet());

    public CleanupMultiFieldViewModel(CleanupPreferences preferences) {
        allJobs = MultiFieldsCleanupPanel.getAllJobs();
        activeJobs.set(preferences.getObservableActiveJobs());
    }
}
