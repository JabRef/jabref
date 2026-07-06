package org.jabref.toolkit.commands;

/// Strategy used to extract references from the "References" section of a PDF.
public enum ExtractionMode {
    RULE_BASED,
    GROBID,
    LLM
}
