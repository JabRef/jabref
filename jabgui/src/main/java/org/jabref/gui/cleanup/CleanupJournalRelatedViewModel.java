package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.cleanup.CleanupPreferences;

public class CleanupJournalRelatedViewModel {
    public static final EnumSet<CleanupPreferences.CleanupStep> CLEANUP_JOURNAL_METHODS = EnumSet.of(
            CleanupPreferences.CleanupStep.ABBREVIATE_DEFAULT,
            CleanupPreferences.CleanupStep.ABBREVIATE_DOTLESS,
            CleanupPreferences.CleanupStep.ABBREVIATE_SHORTEST_UNIQUE,
            CleanupPreferences.CleanupStep.UNABBREVIATE,
            CleanupPreferences.CleanupStep.ABBREVIATION_NO_CHANGES
    );

    public final ObjectProperty<CleanupPreferences.CleanupStep> selectedJournalCleanupOption = new SimpleObjectProperty<>();
    public final CleanupPreferences preferences;

    public CleanupJournalRelatedViewModel(CleanupPreferences preferences) {
        this.preferences = preferences;
        selectedJournalCleanupOption.set(getInitialMethod());
    }

    private CleanupPreferences.CleanupStep getInitialMethod() {
        return CLEANUP_JOURNAL_METHODS.stream()
                                      .filter(preferences::isActive)
                                      .findFirst()
                                      .orElse(CleanupPreferences.CleanupStep.ABBREVIATION_NO_CHANGES);
    }
}
