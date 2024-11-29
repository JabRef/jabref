package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GitStatusTest {
    private Path repositoryPath;
    private GitManager gitManager;
    private GitStatus gitStatus;
    private GitActionExecutor gitActionExecutor;

    private final Logger LOGGER = LoggerFactory.getLogger(GitStatusTest.class);

    @BeforeEach
    void setUp(@TempDir Path temporaryRepository) throws GitException {
        this.repositoryPath = temporaryRepository;
        this.gitManager = GitManager.initGitRepository(repositoryPath);
        this.gitStatus = gitManager.getGitStatus();
        this.gitActionExecutor = gitManager.getGitActionExecutor();
    }

    @Test
    void testTrackedAndUntrackedFilesStatus() throws GitException, IOException {
        assertFalse(gitStatus.hasUntrackedFiles());
        assertFalse(gitStatus.hasTrackedFiles());
        Path pathToTempFile = Files.createTempFile(repositoryPath, null, null);
        assertTrue(gitStatus.hasUntrackedFiles());
        assertFalse(gitStatus.hasTrackedFiles());
        gitActionExecutor.add(pathToTempFile);
        assertFalse(gitStatus.hasUntrackedFiles());
        assertTrue(gitStatus.hasTrackedFiles());
        Path tempDir = Files.createTempDirectory(repositoryPath, null);
        Files.createTempFile(tempDir, null, null);
        assertTrue(gitStatus.hasUntrackedFiles());
        assertTrue(gitStatus.hasTrackedFiles());
        assertEquals(1, gitStatus.getUntrackedFiles().size());
        assertEquals(1, gitStatus.getTrackedFiles().size());
    }
}
