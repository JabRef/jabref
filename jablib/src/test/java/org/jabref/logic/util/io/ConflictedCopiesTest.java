package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [utest->req~ux.external-library-changes.conflicted-copies~1]
class ConflictedCopiesTest {

    private static final Path LIBRARY = Path.of("papers", "library.bib");

    @ParameterizedTest
    @ValueSource(strings = {
            "library (Alice's conflicted copy 2026-09-03).bib",
            "library (conflicted copy 2026-09-03).bib",
            "library (conflicted copy 2026-09-03 143015).bib",
            "library.sync-conflict-20260903-143015-ABCDEFG.bib"
    })
    void recognizesSyncClientNames(String fileName) {
        assertTrue(ConflictedCopies.isConflictedCopy(LIBRARY, LIBRARY.resolveSibling(fileName)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "library.bib",
            "library (1).bib",
            "library-old.bib",
            "other (conflicted copy 2026-09-03).bib",
            "library (conflicted copy 2026-09-03).bib.bak"
    })
    void ignoresOtherNames(String fileName) {
        assertFalse(ConflictedCopies.isConflictedCopy(LIBRARY, LIBRARY.resolveSibling(fileName)));
    }

    @Test
    void findsCopiesNextToTheLibrary(@TempDir Path tempDir) throws IOException {
        Path library = tempDir.resolve("library.bib");
        Path dropbox = tempDir.resolve("library (Alice's conflicted copy 2026-09-03).bib");
        Path syncthing = tempDir.resolve("library.sync-conflict-20260903-143015-ABCDEFG.bib");
        for (Path file : List.of(library, dropbox, syncthing, tempDir.resolve("notes.bib"))) {
            Files.writeString(file, "");
        }

        assertEquals(List.of(dropbox, syncthing), ConflictedCopies.find(library));
    }

    @Test
    void recognizesOneDriveCopyOfTheLocalComputerOnly() {
        ConflictedCopies.localComputerName().ifPresent(computerName -> {
            assertTrue(ConflictedCopies.isConflictedCopy(LIBRARY, LIBRARY.resolveSibling("library-" + computerName + ".bib")));
            assertFalse(ConflictedCopies.isConflictedCopy(LIBRARY, LIBRARY.resolveSibling("library-OTHER-PC.bib")));
        });
    }
}
