package org.jabref.logic.preferences;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.util.io.FileHistory;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class LastFilesOpenedPreferencesTest {

    private final LastFilesOpenedPreferences preferences = new LastFilesOpenedPreferences(
            List.of(Path.of("first.bib"), Path.of("second.bib"), Path.of("third.bib")),
            List.of("Alpha2020", ""),
            null,
            FileHistory.of(List.of()));

    @Test
    void selectedEntryIsFoundByTheLibraryPosition() {
        assertEquals(Optional.of("Alpha2020"), preferences.getLastSelectedEntry(Path.of("first.bib")));
    }

    @Test
    void libraryWithoutSelectionHasNoEntry() {
        assertEquals(Optional.empty(), preferences.getLastSelectedEntry(Path.of("second.bib")));
    }

    @Test
    void libraryBeyondTheStoredSelectionsHasNoEntry() {
        assertEquals(Optional.empty(), preferences.getLastSelectedEntry(Path.of("third.bib")));
    }

    @Test
    void unknownLibraryHasNoEntry() {
        assertEquals(Optional.empty(), preferences.getLastSelectedEntry(Path.of("unknown.bib")));
    }
}
