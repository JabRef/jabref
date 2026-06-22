package org.jabref.gui.cleanup;

import java.util.EnumSet;
import java.util.Optional;

import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.cleanup.CleanupPreferences;

public class CleanupJournalRelatedViewModel {
    public final SetProperty<CleanupPreferences.CleanupStep> selectedJournalCleanupOption = new SimpleSetProperty<>(FXCollections.observableSet());
    public final EnumSet<CleanupPreferences.CleanupStep> allSupportedJobs;
    public final CleanupPreferences preferences;

    public CleanupJournalRelatedViewModel(CleanupPreferences preferences) {
        this.preferences = preferences;

        allSupportedJobs = JournalAbbreviationPanel.getAllCleanupOptions();
        getInitialMethod().ifPresent(selectedJournalCleanupOption::add);
    }

    private Optional<CleanupPreferences.CleanupStep> getInitialMethod() {
        return allSupportedJobs.stream()
                               .filter(preferences::isActive)
                               .findFirst();
    }
}
