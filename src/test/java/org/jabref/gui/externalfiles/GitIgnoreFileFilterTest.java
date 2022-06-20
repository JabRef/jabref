package org.jabref.gui.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class GitIgnoreFileFilterTest {

    @Test
    public void checkSimpleGitIgnore(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve(".gitignore"), """
                *.png
                """);
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertFalse(gitIgnoreFileFilter.accept(dir.resolve("test.png")));
    }

    @Test
    public void checkDirectoryGitIgnore(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve(".gitignore"), """
                **/*.png
                """);
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertFalse(gitIgnoreFileFilter.accept(dir.resolve("test.png")));
    }
}
