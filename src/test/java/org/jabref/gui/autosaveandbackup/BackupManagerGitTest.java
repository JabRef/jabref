package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

class BackupManagerGitTest {

    Path backupDir;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException, GitAPIException {
        backupDir = tempDir.resolve("backup");
    }
}






