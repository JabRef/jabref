package org.jabref.logic.git.merge;

import java.util.List;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.model.MergePlan;

public record MergeAnalysis(
        MergePlan autoPlan,
        List<ThreeWayEntryConflict> conflicts) { }
