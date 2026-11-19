package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
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
                ignore/// *
                """);
        Path subDir = dir.resolve("ignore");
        Files.createDirectories(subDir);
        Files.createFile(subDir.resolve("test.png"));
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertFalse(gitIgnoreFileFilter.accept(subDir.resolve("test.png")));
    }

    // [utest->req~jabgui.externalfiles.unlinked-files.search.gitignore-precedence~1]
    @Test
    void rejectsFileMatchingPatternWithLiteralOpeningBrace(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), """
                notes{draft.pdf
                """);
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertFalse(gitIgnoreFileFilter.accept(dir.resolve("notes{draft.pdf")));
    }

    @Test
    void acceptsOtherFilesWhenPatternContainsLiteralOpeningBrace(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), """
                notes{draft.pdf
                """);
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertTrue(gitIgnoreFileFilter.accept(dir.resolve("paper.pdf")));
    }

    @Test
    void acceptsDefaultIgnoredFileWhenNegatedInGitIgnore(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), """
                !desktop.ini
                """);
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertTrue(gitIgnoreFileFilter.accept(dir.resolve("desktop.ini")));
    }

    @Test
    void rejectsDefaultIgnoredFileWithoutGitIgnore(@TempDir Path dir) throws IOException {
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertFalse(gitIgnoreFileFilter.accept(dir.resolve("Thumbs.db")));
    }

    // [utest->req~jabgui.externalfiles.unlinked-files.search.gitignore-symlink~1]
    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "Creating symbolic links may require elevated privileges")
    void directoryOnlyRuleDoesNotIgnoreSymbolicLink(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), "link/\n");
        Path target = Files.createDirectory(dir.resolve("target"));
        Path link = Files.createSymbolicLink(dir.resolve("link"), target);

        assertTrue(new GitIgnoreFileFilter(dir).accept(link));
    }

    // [utest->req~jabgui.externalfiles.unlinked-files.search.gitignore-precedence~1]
    @Test
    void nestedIgnoreRetainsParentNegationAndOverridesParentRule(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), "!desktop.ini\n*.pdf\n");
        Path nested = Files.createDirectory(dir.resolve("nested"));
        Files.writeString(nested.resolve(".gitignore"), "!keep.pdf\n");
        GitIgnoreFileFilter filter = new GitIgnoreFileFilter(nested);

        assertTrue(filter.accept(nested.resolve("desktop.ini")));
        assertTrue(filter.accept(nested.resolve("keep.pdf")));
        assertFalse(filter.accept(nested.resolve("other.pdf")));
    }

    @Test
    void subdirectoriesReuseParentRulesWithoutSharingTheirOwnRules(@TempDir Path dir) throws IOException {
        Path parentIgnore = dir.resolve(".gitignore");
        Files.writeString(parentIgnore, "*.pdf\n");
        GitIgnoreFileFilter parent = new GitIgnoreFileFilter(dir);
        Files.delete(parentIgnore);
        Path first = Files.createDirectory(dir.resolve("first"));
        Files.writeString(first.resolve(".gitignore"), "!keep.pdf\n");
        Path second = Files.createDirectory(dir.resolve("second"));

        GitIgnoreFileFilter firstFilter = parent.forSubdirectory(first);
        GitIgnoreFileFilter secondFilter = parent.forSubdirectory(second);
        assertTrue(firstFilter.accept(first.resolve("keep.pdf")));
        assertFalse(firstFilter.accept(first.resolve("other.pdf")));
        assertFalse(secondFilter.accept(second.resolve("keep.pdf")));
    }

    @Test
    void normalizesSearchAndCandidatePathsBeforeApplyingAncestorRules(@TempDir Path dir) throws IOException {
        Path other = Files.createDirectory(dir.resolve("other"));
        Files.writeString(other.resolve(".gitignore"), "*.pdf\n");
        Path target = Files.createDirectory(dir.resolve("target"));
        Files.writeString(target.resolve(".gitignore"), "/ignored.pdf\n");
        Path searchPath = other.resolve("..").resolve("target");

        GitIgnoreFileFilter filter = new GitIgnoreFileFilter(searchPath);

        assertTrue(filter.accept(searchPath.resolve("allowed.pdf")));
        assertFalse(filter.accept(searchPath.resolve("ignored.pdf")));
        assertFalse(filter.accept(target.resolve("ignored.pdf")));
    }

    // [utest->req~jabgui.externalfiles.unlinked-files.search.gitignore-windows-case~1]
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void windowsMatchesPatternsIgnoringCase(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), "*.pdf\n");
        assertFalse(new GitIgnoreFileFilter(dir).accept(dir.resolve("UPPER.PDF")));
    }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "\\ is an invalid filename character in Windows")
    void acceptsFileWithBackslashInNameWhenPatternLooksLikeDirectory(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".gitignore"), """
                notes/draft.pdf
                """);
        GitIgnoreFileFilter gitIgnoreFileFilter = new GitIgnoreFileFilter(dir);
        assertTrue(gitIgnoreFileFilter.accept(dir.resolve("notes\\draft.pdf")));
    }
}
