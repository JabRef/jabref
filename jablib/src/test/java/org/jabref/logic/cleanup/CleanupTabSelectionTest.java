package org.jabref.logic.cleanup;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CleanupTabSelectionTest {

    private CleanupPreferences cleanupPreferences;

    @BeforeEach
    void setUp() {
        cleanupPreferences = new CleanupPreferences(EnumSet.of(
                CleanupPreferences.CleanupStep.CLEAN_UP_DOI
        ));
    }

    @Test
    void updatePreferencesReplacesStepsInSameCategory() {
        EnumSet<CleanupPreferences.CleanupStep> allJobs = EnumSet.of(
                CleanupPreferences.CleanupStep.CLEAN_UP_DOI,
                CleanupPreferences.CleanupStep.CLEANUP_EPRINT
        );
        EnumSet<CleanupPreferences.CleanupStep> selectedJobs = EnumSet.of(
                CleanupPreferences.CleanupStep.CLEANUP_EPRINT
        );

        CleanupTabSelection selection = CleanupTabSelection.ofJobs(allJobs, selectedJobs);
        CleanupPreferences result = selection.updatePreferences(cleanupPreferences);

        EnumSet<CleanupPreferences.CleanupStep> expected = EnumSet.of(CleanupPreferences.CleanupStep.CLEANUP_EPRINT);
        assertEquals(expected, result.getActiveJobs());
    }

    @Test
    void updatePreferencesAddsStepInDifferentCategory() {
        EnumSet<CleanupPreferences.CleanupStep> allJobs = EnumSet.of(
                CleanupPreferences.CleanupStep.MOVE_PDF,
                CleanupPreferences.CleanupStep.MAKE_PATHS_RELATIVE
        );
        EnumSet<CleanupPreferences.CleanupStep> selectedJobs = EnumSet.of(
                CleanupPreferences.CleanupStep.MOVE_PDF
        );

        CleanupTabSelection selection = CleanupTabSelection.ofJobs(allJobs, selectedJobs);
        CleanupPreferences result = selection.updatePreferences(cleanupPreferences);

        EnumSet<CleanupPreferences.CleanupStep> expected = EnumSet.of(
                CleanupPreferences.CleanupStep.CLEAN_UP_DOI,
                CleanupPreferences.CleanupStep.MOVE_PDF
        );
        assertEquals(expected, result.getActiveJobs());
    }

    @Test
    void updatePreferencesAppliesFormatterCleanups() {
        FieldFormatterCleanupActions formatter = new FieldFormatterCleanupActions(true, FieldFormatterCleanupMapper.parseActions("title[identity]"));

        CleanupTabSelection selection = CleanupTabSelection.ofFormatters(formatter);
        CleanupPreferences result = selection.updatePreferences(cleanupPreferences);

        assertEquals(formatter, result.getFieldFormatterCleanups());
    }
}
