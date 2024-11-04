package org.jabref.logic.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GitManagerTest {

    private GitManager gitManager;
    private Path tempRepositoryPath;

    @BeforeEach
        // since @Tempdir is called on a parameter of a @BeforeEach method, it is avilable for all methods of the class
        // see junit docs: https://junit.org/junit5/docs/5.4.1/api/org/junit/jupiter/api/io/TempDir.html
    void setUp(@TempDir Path tempDir) {
        this.tempRepositoryPath = tempDir;
        this.gitManager = new GitManager(tempRepositoryPath);
        // this constructor calls initGitRepository, since we are using a temdir, which is not a git repo
        // the git repository must be created.
    }


    @Test
    void testInitGitRepository_createsNewRepositoryWhenNoneExists() {
        // since the temdir is not a git repo we check if the constructor call above made a git repo out of it:
        assertTrue(gitManager.isGitRepository());
        assertNotNull(gitManager.getGitActionExecutor());
    }

    @Test
    void testInitGitRepository_opensExistingRepository(@TempDir Path tempDir) throws GitAPIException, IOException {
        try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
            // manually created Git repository
            GitManager gitManager = new GitManager(tempRepositoryPath);

            assertTrue(gitManager.isGitRepository());
            assertNotNull(gitManager.getGitActionExecutor());
        }

    }

}
