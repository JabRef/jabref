package org.jabref.logic.util.io;

/// A user-configured string-prefix substitution used to resolve absolute linked-file paths that were stored on a
/// different machine (e.g. a different OS, or a differently mounted network share).
///
/// @param directory        the stored path prefix to match (plain string, no regex)
/// @param mappedDirectory  the replacement prefix to try on this machine
public record DirectoryMapping(String directory, String mappedDirectory) {
}
