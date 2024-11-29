package org.jabref.logic.git;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

import org.jabref.logic.shared.security.Password;

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
    private GitPreferences gitPreferences;
    private GitManager gitManager;
    private GitStatus gitStatus;
    private GitActionExecutor gitActionExecutor;

    private final Logger LOGGER = LoggerFactory.getLogger(GitStatusTest.class);

    @BeforeEach
    void setUp(@TempDir Path temporaryRepository) throws GitException, GeneralSecurityException, UnsupportedEncodingException {
        this.repositoryPath = temporaryRepository;
        this.gitPreferences = new GitPreferences(true, "username",
                new Password("password".toCharArray(), "username").encrypt(), false,
                "", false, false);
        this.gitManager = GitManager.initGitRepository(repositoryPath, gitPreferences);
        this.gitStatus = gitManager.getGitStatus();
        this.gitActionExecutor = gitManager.getGitActionExecutor();
    }

    @Test
    void trackedAndUntrackedFilesStatus() throws GitException, IOException {
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
