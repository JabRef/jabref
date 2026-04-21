package org.jabref.logic.refcheck;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.Nullable;

public record RefCheckResult(RefValidity validity, @Nullable BibEntry otherEntry, double similarityScore) {
}
