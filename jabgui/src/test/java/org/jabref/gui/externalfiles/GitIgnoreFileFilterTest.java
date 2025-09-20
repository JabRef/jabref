package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitIgnoreFileFilterTest {

    @Test
    void checkSimpleGitIgnore(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), """
                *.png
                """);
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertFalse(gitIgnoreFileFilter.accept(dir.resolve("test.png")));
    }

    @Test
    void checkSimpleGitIgnoreWithAllowing(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), """
                !*.png
                """);
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertTrue(gitIgnoreFileFilter.accept(dir.resolve("test.png")));
    }

    @Test
    void checkSimpleGitIgnoreWithOverwritingDefs(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), """
                !*.png
                *.png
                """);
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertFalse(gitIgnoreFileFilter.accept(dir.resolve("test.png")));
    }

    @Test
    void checkDirectoryGitIgnore(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), """
                **/*.png
                """);
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertFalse(gitIgnoreFileFilter.accept(dir.resolve("test.png")));
    }

    @Test
    void checkDirectoryGitIgnoreSubDir(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), """
                ignore/.*
                ignore/*
                ignore/**
                ignore/**/*
                """);
        Path subDir = dir.resolve("ignore");
        Files.createDirectories(subDir);
        Files.createFile(subDir.resolve("test.png"));
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertFalse(gitIgnoreFileFilter.accept(subDir.resolve("test.png")));
    }
}
