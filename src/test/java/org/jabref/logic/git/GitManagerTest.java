package org.jabref.logic.git;

import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitManagerTest {

    private Path tempPath;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws GitException {
        this.tempPath = tempDir;
    }

    @Test
    void initGitRepositoryCreatesNewRepositoryWhenNoneExists() throws GitException {
        assertFalse(GitManager.isGitRepository(tempPath));
        GitException exception = assertThrows(GitException.class, () -> GitManager.openGitRepository(this.tempPath));
        assertEquals(tempPath.getFileName() + " is not a git repository.", exception.getMessage());
        assertDoesNotThrow(() -> GitManager.initGitRepository(tempPath));
        assertTrue(GitManager.isGitRepository(tempPath));
    }

    @Test
    void initGitRepositoryOpensExistingRepository() throws GitAPIException {
        // manually create Git repository
        try (Git git = Git.init().setDirectory(tempPath.toFile()).call()) {
            GitException exception = assertThrows(GitException.class, () -> GitManager.initGitRepository(tempPath));
            assertEquals(tempPath.getFileName() + " is already a git repository.", exception.getMessage());
            assertDoesNotThrow(() -> GitManager.openGitRepository(tempPath));
            assertTrue(GitManager.isGitRepository(tempPath));
        }
    }
}
