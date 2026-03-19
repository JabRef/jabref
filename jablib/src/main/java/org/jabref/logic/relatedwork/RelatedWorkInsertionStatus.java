package org.jabref.logic.relatedwork;

public enum RelatedWorkInsertionStatus {
    /// Insert successfully
    INSERTED,

    /// `[citation-key]`: `xxxxx` already exists, not inserted
    UNCHANGED,

    /// Cannot find a matched related work
    SKIPPED,
}
