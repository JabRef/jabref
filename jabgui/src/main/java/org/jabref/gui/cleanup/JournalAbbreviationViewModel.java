package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.cleanup.CleanupPreferences;

public class JournalAbbreviationViewModel {
    public static final EnumSet<CleanupPreferences.CleanupStep> CLEANUP_JOURNAL_METHODS = EnumSet.of(
            CleanupPreferences.CleanupStep.ABBREVIATE_DEFAULT,
            CleanupPreferences.CleanupStep.ABBREVIATE_DOTLESS,
            CleanupPreferences.CleanupStep.ABBREVIATE_SHORTEST_UNIQUE,
            CleanupPreferences.CleanupStep.ABBREVIATE_LTWA,
            CleanupPreferences.CleanupStep.UNABBREVIATE
    );

    public final SetProperty<CleanupPreferences.CleanupStep> selectedJournalCleanupOption = new SimpleSetProperty<>(FXCollections.observableSet());

    public JournalAbbreviationViewModel() {
        selectedJournalCleanupOption.set(null);
    }
}
