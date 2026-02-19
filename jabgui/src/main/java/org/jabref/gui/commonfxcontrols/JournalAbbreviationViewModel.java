package org.jabref.gui.commonfxcontrols;

import java.util.EnumSet;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.cleanup.CleanupPreferences;

public class JournalAbbreviationViewModel {
    public static final EnumSet<CleanupPreferences.CleanupStep> CLEANUP_JOURNAL_METHODS = EnumSet.of(
            CleanupPreferences.CleanupStep.ABBREVIATE_DEFAULT,
            CleanupPreferences.CleanupStep.ABBREVIATE_DOTLESS,
            CleanupPreferences.CleanupStep.ABBREVIATE_SHORTEST_UNIQUE,
            CleanupPreferences.CleanupStep.ABBREVIATE_LTWA,
            CleanupPreferences.CleanupStep.UNABBREVIATE
    );

    public final ObjectProperty<CleanupPreferences.CleanupStep> selectedJournalCleanupOption = new SimpleObjectProperty<>();

    public JournalAbbreviationViewModel() {
        selectedJournalCleanupOption.set(CleanupPreferences.CleanupStep.ABBREVIATE_DEFAULT);
    }
}
