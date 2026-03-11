package org.jabref.logic.relatedwork;

/// INSERTED: Insert successfully
/// UNCHANGED: [citation-key]: xxxxx already exists, do not insert
/// SKIPPED: Cannot find a matched related work
public enum RelatedWorkInsertionStatus {
    INSERTED,
    UNCHANGED,
    SKIPPED,
}
