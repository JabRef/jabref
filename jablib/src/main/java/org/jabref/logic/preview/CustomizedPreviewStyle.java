package org.jabref.logic.preview;

// A persisted, record of a custom citation style.
// Meant to act as a persisted snapshot of the data needed to reconstruct a TextBasedPreviewLayout
public record CustomizedPreviewStyle(String id, String name, String text) {
}
