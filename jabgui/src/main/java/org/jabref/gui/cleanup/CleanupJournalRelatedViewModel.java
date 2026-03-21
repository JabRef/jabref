package org.jabref.gui.cleanup;

import java.util.EnumSet;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.cleanup.CleanupPreferences;

public class CleanupJournalRelatedViewModel {
    public final ObjectProperty<Optional<CleanupPreferences.CleanupStep>> selectedJournalCleanupOption = new SimpleObjectProperty<>();
    public final EnumSet<CleanupPreferences.CleanupStep> allSupportedJobs;
    public final CleanupPreferences preferences;

    public CleanupJournalRelatedViewModel(CleanupPreferences preferences) {
        this.preferences = preferences;

        allSupportedJobs = JournalAbbreviationPanel.getAllCleanupOptions();
        selectedJournalCleanupOption.set(getInitialMethod());
    }

    private Optional<CleanupPreferences.CleanupStep> getInitialMethod() {
        return allSupportedJobs.stream()
                               .filter(preferences::isActive)
                               .findFirst();
    }
}
