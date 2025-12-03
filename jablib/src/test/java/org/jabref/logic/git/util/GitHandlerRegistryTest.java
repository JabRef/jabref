package org.jabref.logic.git.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.preferences.GitPreferences;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GitHandlerRegistryTest {
    @TempDir
    Path tempDir;

    private GitHandlerRegistry registry;
    private Git testGit;

    @BeforeEach
    void setUp() {
        SystemReader.setInstance(new NoopGitSystemReader());
        GitPreferences gitPreferences = mock(GitPreferences.class, Answers.RETURNS_DEEP_STUBS);
        registry = new GitHandlerRegistry(gitPreferences);
    }

    @AfterEach
    void tearDown() {
        if (testGit != null) {
            testGit.close();
        }

        RepositoryCache.clear();
        WindowCache.reconfigure(new WindowCacheConfig());
    }

    @Test
    void returnsSameHandlerForSameRepoPath() throws Exception {
        testGit = Git.init().setDirectory(tempDir.toFile()).call();
        Path repoPath = tempDir.toAbsolutePath().normalize();

        GitHandler handler1 = registry.get(repoPath);
        GitHandler handler2 = registry.get(repoPath);

        assertSame(handler1, handler2);
    }

    @Test
    void returnsEmptyForNonGitPath() {
        Path nonGitPath = tempDir.resolve("non-git-folder");
        assertFalse(registry.fromAnyPath(nonGitPath).isPresent());
    }

    @Test
    void resolvesHandlerFromNestedPath() throws Exception {
        testGit = Git.init().setDirectory(tempDir.toFile()).call();

        Path subDir = Files.createDirectory(tempDir.resolve("nested"));
        Optional<GitHandler> handlerOpt = registry.fromAnyPath(subDir);

        assertTrue(handlerOpt.isPresent(), "Should resolve handler from subdirectory inside repo");

        GitHandler handler1 = registry.get(tempDir);
        GitHandler handler2 = handlerOpt.get();
        assertSame(handler1, handler2, "Should return same cached handler");
    }
}
