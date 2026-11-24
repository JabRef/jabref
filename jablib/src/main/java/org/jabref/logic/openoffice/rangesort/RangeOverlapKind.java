package org.jabref.logic.openoffice.rangesort;

public enum RangeOverlapKind {

    /// The ranges share a boundary
    TOUCH,

    /// They share some characters
    OVERLAP,

    /// They cover the same XTextRange
    EQUAL_RANGE
}

