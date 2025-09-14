package org.jabref.logic.cleanup;

import java.util.EnumSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CleanupPreferencesTest {

    private CleanupPreferences cleanupPreferences;

    @BeforeEach
    void setUp() {
        cleanupPreferences = new CleanupPreferences(EnumSet.of(
                CleanupPreferences.CleanupStep.CLEAN_UP_DOI
        ));
    }

    @Test
    void updateWithReplacesStepsInSameCategory() {
        EnumSet<CleanupPreferences.CleanupStep> allJobs = EnumSet.of(
                CleanupPreferences.CleanupStep.CLEAN_UP_DOI,
                CleanupPreferences.CleanupStep.CLEANUP_EPRINT
        );
        EnumSet<CleanupPreferences.CleanupStep> selectedJobs = EnumSet.of(
                CleanupPreferences.CleanupStep.CLEANUP_EPRINT
        );

        CleanupPreferences result = cleanupPreferences.updateWith(
                Optional.of(allJobs),
                Optional.of(selectedJobs),
                Optional.empty()
        );

        EnumSet<CleanupPreferences.CleanupStep> expected = EnumSet.of(CleanupPreferences.CleanupStep.CLEANUP_EPRINT);
        assertEquals(expected, result.getActiveJobs());
    }

    @Test
    void updateWithAddsStepInDifferentCategory() {
        EnumSet<CleanupPreferences.CleanupStep> allJobs = EnumSet.of(
                CleanupPreferences.CleanupStep.MOVE_PDF,
                CleanupPreferences.CleanupStep.MAKE_PATHS_RELATIVE
        );
        EnumSet<CleanupPreferences.CleanupStep> selectedJobs = EnumSet.of(
                CleanupPreferences.CleanupStep.MOVE_PDF
        );

        CleanupPreferences result = cleanupPreferences.updateWith(
                Optional.of(allJobs),
                Optional.of(selectedJobs),
                Optional.empty()
        );

        EnumSet<CleanupPreferences.CleanupStep> expected = EnumSet.of(
                CleanupPreferences.CleanupStep.CLEAN_UP_DOI,
                CleanupPreferences.CleanupStep.MOVE_PDF
        );
        assertEquals(expected, result.getActiveJobs());
    }
}
