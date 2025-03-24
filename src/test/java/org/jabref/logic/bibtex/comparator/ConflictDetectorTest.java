package org.jabref.logic.bibtex.comparator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ConflictDetectorTest {

    private BibDatabaseContext base;
    private BibDatabaseContext local;
    private BibDatabaseContext remote;
    private GitBibDatabaseDiff diff;
    private GitBibEntryDiff entryDiff;

    @BeforeEach
    void setUp() {
        base = mock(BibDatabaseContext.class);
        local = mock(BibDatabaseContext.class);
        remote = mock(BibDatabaseContext.class);
        diff = mock(GitBibDatabaseDiff.class);
        entryDiff = mock(GitBibEntryDiff.class);
    }

    @Test
    void testDetectGitConflictsNoConflicts() {
        when(diff.getEntryDifferences()).thenReturn(Collections.emptyList());
        when(diff.getMetaDataDifferences()).thenReturn(Optional.empty());

        try (var mockedStatic = mockStatic(GitBibDatabaseDiff.class)) {
            mockedStatic.when(() -> GitBibDatabaseDiff.compare(base, local, remote)).thenReturn(diff);
            assertEquals(Optional.empty(), ConflictDetector.detectGitConflicts(base, local, remote));
        }
    }

    @Test
    void testDetectGitConflictsWithEntryConflicts() {
        when(entryDiff.hasConflicts()).thenReturn(true);
        when(diff.getEntryDifferences()).thenReturn(List.of(entryDiff));
        when(diff.getMetaDataDifferences()).thenReturn(Optional.empty());

        try (var mockedStatic = mockStatic(GitBibDatabaseDiff.class)) {
            mockedStatic.when(() -> GitBibDatabaseDiff.compare(base, local, remote)).thenReturn(diff);
            assertTrue(ConflictDetector.detectGitConflicts(base, local, remote).isPresent());
        }
    }

    @Test
    void testDetectGitConflictsWithMetaDataConflicts() {
        when(diff.getEntryDifferences()).thenReturn(Collections.emptyList());
        when(diff.getMetaDataDifferences()).thenReturn(Optional.of(mock(MetaDataDiff.class)));

        try (var mockedStatic = mockStatic(GitBibDatabaseDiff.class)) {
            mockedStatic.when(() -> GitBibDatabaseDiff.compare(base, local, remote)).thenReturn(diff);
            assertTrue(ConflictDetector.detectGitConflicts(base, local, remote).isPresent());
        }
    }

    @Test
    void testHasGitConflictsTrue() {
        when(diff.getEntryDifferences()).thenReturn(List.of(entryDiff));
        when(entryDiff.hasConflicts()).thenReturn(true);

        try (var mockedStatic = mockStatic(GitBibDatabaseDiff.class)) {
            mockedStatic.when(() -> GitBibDatabaseDiff.compare(base, local, remote)).thenReturn(diff);
            assertTrue(ConflictDetector.hasGitConflicts(base, local, remote));
        }
    }

    @Test
    void testHasGitConflictsFalse() {
        when(diff.getEntryDifferences()).thenReturn(Collections.emptyList());
        when(diff.getMetaDataDifferences()).thenReturn(Optional.empty());

        try (var mockedStatic = mockStatic(GitBibDatabaseDiff.class)) {
            mockedStatic.when(() -> GitBibDatabaseDiff.compare(base, local, remote)).thenReturn(diff);
            assertFalse(ConflictDetector.hasGitConflicts(base, local, remote));
        }
    }

    @Test
    void testGetGitEntryConflicts() {
        when(diff.getEntryDifferences()).thenReturn(List.of(entryDiff));

        try (var mockedStatic = mockStatic(GitBibDatabaseDiff.class)) {
            mockedStatic.when(() -> GitBibDatabaseDiff.compare(base, local, remote)).thenReturn(diff);
            List<GitBibEntryDiff> conflicts = ConflictDetector.getGitEntryConflicts(base, local, remote);
            assertEquals(1, conflicts.size());
            assertEquals(entryDiff, conflicts.getFirst());
        }
    }
}

