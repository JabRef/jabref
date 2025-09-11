package org.jabref.logic.cleanup;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanupPreferencesTest {

    private CleanupPreferences cleanupPreferences;

    @BeforeEach
    void setUp() {
        cleanupPreferences = new CleanupPreferences(EnumSet.of(CleanupPreferences.CleanupStep.CLEAN_UP_DOI));
    }

    @Test
    void updateWithReplacesStepsInSameCategory_RemovesOldStep() {
        CleanupPreferences update = new CleanupPreferences(EnumSet.of(CleanupPreferences.CleanupStep.CLEANUP_EPRINT));
        CleanupPreferences result = cleanupPreferences.updateWith(update);
        Set<CleanupPreferences.CleanupStep> activeJobs = result.getActiveJobs();

        assertFalse(activeJobs.contains(CleanupPreferences.CleanupStep.CLEAN_UP_DOI));
    }

    @Test
    void updateWithReplacesStepsInSameCategory_AddsNewStep() {
        CleanupPreferences update = new CleanupPreferences(EnumSet.of(CleanupPreferences.CleanupStep.CLEANUP_EPRINT));
        CleanupPreferences result = cleanupPreferences.updateWith(update);
        Set<CleanupPreferences.CleanupStep> activeJobs = result.getActiveJobs();

        assertTrue(activeJobs.contains(CleanupPreferences.CleanupStep.CLEANUP_EPRINT));
    }

    @Test
    void updateWithDifferentCategory_KeepsOldStep() {
        CleanupPreferences update = new CleanupPreferences(EnumSet.of(CleanupPreferences.CleanupStep.MOVE_PDF));
        CleanupPreferences result = cleanupPreferences.updateWith(update);
        Set<CleanupPreferences.CleanupStep> activeJobs = result.getActiveJobs();

        assertTrue(activeJobs.contains(CleanupPreferences.CleanupStep.CLEAN_UP_DOI));
    }

    @Test
    void updateWithDifferentCategory_AddsNewStep() {
        CleanupPreferences update = new CleanupPreferences(EnumSet.of(CleanupPreferences.CleanupStep.MOVE_PDF));
        CleanupPreferences result = cleanupPreferences.updateWith(update);
        Set<CleanupPreferences.CleanupStep> activeJobs = result.getActiveJobs();

        assertTrue(activeJobs.contains(CleanupPreferences.CleanupStep.MOVE_PDF));
    }
}
