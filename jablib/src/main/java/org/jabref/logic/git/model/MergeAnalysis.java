package org.jabref.logic.git.model;

import java.util.List;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;

public record MergeAnalysis(
        MergePlan autoPlan,
        List<ThreeWayEntryConflict> conflicts) {
}
