package org.jabref.toolkit.commands;

import org.jspecify.annotations.NullMarked;

/// Strategy used to extract references from the "References" section of a PDF.
@NullMarked
public enum ExtractionMode {
    RULE_BASED,
    GROBID,
    LLM
}
