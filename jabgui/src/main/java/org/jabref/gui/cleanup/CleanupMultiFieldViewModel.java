package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.commonfxcontrols.MultiFieldsCleanupPanel;
import org.jabref.logic.cleanup.CleanupPreferences;

public class CleanupMultiFieldViewModel {
    public final SetProperty<CleanupPreferences.CleanupStep> activeJobs = new SimpleSetProperty<>(FXCollections.observableSet());
    public final EnumSet<CleanupPreferences.CleanupStep> allSupportedJobs;

    public CleanupMultiFieldViewModel(CleanupPreferences preferences) {
        this.allSupportedJobs = MultiFieldsCleanupPanel.getAllJobs();

        EnumSet<CleanupPreferences.CleanupStep> initialSet = EnumSet.copyOf(preferences.getObservableActiveJobs());
        initialSet.retainAll(allSupportedJobs);
        activeJobs.addAll(initialSet);
    }
}
