package org.jabref.logic.preview;

// A persisted, record of a custom citation preview style.
// id introduced as an uuid to help with keeping track of renaming custom citations
// CustomizedTextPreviewLayout doesn't implement PreviewLayout
// Meant to act as a persisted snapshot of the data needed to reconstruct a TextBasedPreviewLayout
public record CustomizedPreviewStyle(String id, String name, String text) {
}
