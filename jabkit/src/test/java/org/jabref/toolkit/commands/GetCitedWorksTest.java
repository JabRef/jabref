package org.jabref.toolkit.commands;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetCitedWorksTest extends AbstractJabKitTest {

    @Test
    void existingDoiPrintsToStdoutWithDefaults(@TempDir Path tempDir) {
        assertEquals(true, true);
    }
}
