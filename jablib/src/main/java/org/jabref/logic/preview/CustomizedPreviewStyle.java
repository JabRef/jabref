package org.jabref.logic.preview;

import io.github.thibaultmeyer.cuid.CUID;
import org.jspecify.annotations.NullMarked;

// A persisted record of a custom citation preview style.
// id introduced as an uuid to help with keeping track of renaming custom citations
// Meant to act as a persisted snapshot of the data needed to reconstruct a TextBasedPreviewLayout
@NullMarked
public record CustomizedPreviewStyle(String id, String name, String text) {
    private static final int CUID_LENGTH = 12;

    public CustomizedPreviewStyle(String name, String text) {
        this(CUID.randomCUID2(CUID_LENGTH).toString(), name, text);
    }
}
