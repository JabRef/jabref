package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlrGitHandlerTest {
    @TempDir
    Path repositoryPath;
    private SlrGitHandler gitHandler;

    @BeforeEach
    public void setUpGitHandler() {
        gitHandler = new SlrGitHandler(repositoryPath);
    }

    @Test
    void calculateDiffOnBranch() throws IOException, GitAPIException {
        String expectedPatch =
                "diff --git a/TestFolder/Test1.txt b/TestFolder/Test1.txt\n" +
                        "index 74809e3..2ae1945 100644\n" +
                        "--- a/TestFolder/Test1.txt\n" +
                        "+++ b/TestFolder/Test1.txt\n" +
                        "@@ -1 +1,2 @@\n" +
                        "+This is a new line of text 2\n" +
                        " This is a new line of text\n";

        gitHandler.checkoutBranch("branch1");
        Files.createDirectory(Path.of(repositoryPath.toString(), "TestFolder"));
        Files.createFile(Path.of(repositoryPath.toString(), "TestFolder", "Test1.txt"));
        Files.writeString(Path.of(repositoryPath.toString(), "TestFolder", "Test1.txt"), "This is a new line of text\n");
        gitHandler.createCommitOnCurrentBranch("Commit 1 on branch1", false);

        Files.createFile(Path.of(repositoryPath.toString(), "Test2.txt"));
        Files.writeString(Path.of(repositoryPath.toString(), "TestFolder", "Test1.txt"), "This is a new line of text 2\n" + Files.readString(Path.of(repositoryPath.toString(), "TestFolder", "Test1.txt")));
        gitHandler.createCommitOnCurrentBranch("Commit 2 on branch1", false);

        System.out.println(gitHandler.calculatePatchOfNewSearchResults("branch1"));
        assertEquals(expectedPatch, gitHandler.calculatePatchOfNewSearchResults("branch1"));
    }

    @Test
    void calculatePatch() throws IOException, GitAPIException {
        Map<Path, String> expected = new HashMap<>();
        expected.put(Path.of(repositoryPath.toString(), "TestFolder", "Test1.txt"), "This is a new line of text 2");

        Map<Path, String> result = gitHandler.parsePatchForAddedEntries(
                "diff --git a/TestFolder/Test1.txt b/TestFolder/Test1.txt\n" +
                        "index 74809e3..2ae1945 100644\n" +
                        "--- a/TestFolder/Test1.txt\n" +
                        "+++ b/TestFolder/Test1.txt\n" +
                        "@@ -1 +1,2 @@\n" +
                        "+This is a new line of text 2\n" +
                        " This is a new line of text");

        assertEquals(expected, result);
    }

    @Test
    void applyPatch() throws IOException, GitAPIException {
        gitHandler.checkoutBranch("branch1");
        Files.createFile(Path.of(repositoryPath.toString(), "Test1.txt"));
        gitHandler.createCommitOnCurrentBranch("Commit on branch1", false);
        gitHandler.checkoutBranch("branch2");
        Files.createFile(Path.of(repositoryPath.toString(), "Test2.txt"));
        Files.writeString(Path.of(repositoryPath.toString(), "Test1.txt"), "This is a new line of text");
        gitHandler.createCommitOnCurrentBranch("Commit on branch2.", false);

        gitHandler.checkoutBranch("branch1");
        gitHandler.appendLatestSearchResultsOntoCurrentBranch("TestMessage", "branch2");

        assertEquals("This is a new line of text", Files.readString(Path.of(repositoryPath.toString(), "Test1.txt")));
    }
}
