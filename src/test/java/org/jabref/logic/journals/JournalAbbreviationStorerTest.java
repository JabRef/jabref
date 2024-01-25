package org.jabref.logic.journals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JournalAbbreviationStorerTest {

    @Test
    public void shouldStoreMVFile() throws IOException {
        File csvFile = new File("test.csv");
        assertTrue(csvFile.createNewFile());
        JournalAbbreviationStorer.store(Path.of(csvFile.getAbsolutePath()));
        File mvFile = new File("test.mv");
        assertTrue(mvFile.exists());
        assertTrue(csvFile.delete());
        assertTrue(mvFile.delete());
    }
}
