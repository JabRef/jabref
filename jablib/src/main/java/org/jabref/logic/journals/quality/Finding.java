package org.jabref.logic.journals.quality;

/**
 * Represents one issue detected by a checker.
 */
public record Finding(Severity severity, String code, String message, AbbreviationEntry entry) {
}
