package org.jabref.gui.cleanup;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanups;

public record CleanupTabSelection(
        EnumSet<CleanupPreferences.CleanupStep> allJobs,
        EnumSet<CleanupPreferences.CleanupStep> selectedJobs,
        Optional<FieldFormatterCleanups> formatters) {

    public CleanupTabSelection {
        allJobs = (allJobs == null) ? EnumSet.noneOf(CleanupPreferences.CleanupStep.class) : EnumSet.copyOf(allJobs);
        selectedJobs = (selectedJobs == null) ? EnumSet.noneOf(CleanupPreferences.CleanupStep.class) : EnumSet.copyOf(selectedJobs);
        formatters = Objects.requireNonNullElse(formatters, Optional.empty());
    }

    public static CleanupTabSelection ofJobs(EnumSet<CleanupPreferences.CleanupStep> allJobs, EnumSet<CleanupPreferences.CleanupStep> selectedJobs) {
        return new CleanupTabSelection(allJobs, selectedJobs, Optional.empty());
    }

    public static CleanupTabSelection ofFormatters(FieldFormatterCleanups cleanups) {
        return new CleanupTabSelection(EnumSet.noneOf(CleanupPreferences.CleanupStep.class), EnumSet.noneOf(CleanupPreferences.CleanupStep.class), Optional.of(cleanups));
    }
}
