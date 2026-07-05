package org.jabref.model.entry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.io.DirectoryMapping;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinkedFileTest {

    // An absolute path from a foreign machine, unlikely to exist locally as-is.
    private static final String FOREIGN_ABSOLUTE_LINK = (OS.WINDOWS ? "C:\\old\\literature\\paper.pdf" : "/old/literature/paper.pdf");
    private static final String FOREIGN_DIRECTORY = (OS.WINDOWS ? "C:\\old\\literature" : "/old/literature");

    @Test
    void findInAppliesDirectoryMappingWhenAbsoluteLinkIsNotFoundDirectly(@TempDir Path temp) throws IOException {
        Path mappedDir = Files.createDirectories(temp.resolve("mapped"));
        Path mappedFile = Files.createFile(mappedDir.resolve("paper.pdf"));

        FilePreferences filePreferences = FilePreferences.getDefault();
        filePreferences.setDirectoryMappings(List.of(new DirectoryMapping(FOREIGN_DIRECTORY, mappedDir.toString())));

        LinkedFile linkedFile = new LinkedFile("", FOREIGN_ABSOLUTE_LINK, "PDF");
        BibDatabaseContext databaseContext = new BibDatabaseContext();

        assertEquals(Optional.of(mappedFile), linkedFile.findIn(databaseContext, filePreferences));
    }

    @Test
    void findInReturnsEmptyWhenNoDirectoryMappingMatches() {
        FilePreferences filePreferences = FilePreferences.getDefault();

        LinkedFile linkedFile = new LinkedFile("", FOREIGN_ABSOLUTE_LINK, "PDF");
        BibDatabaseContext databaseContext = new BibDatabaseContext();

        assertEquals(Optional.empty(), linkedFile.findIn(databaseContext, filePreferences));
    }
}
