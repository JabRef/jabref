package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitStatusTest {

    private Path repositoryPath;
    private Git git;
    private GitStatus gitStatus;

    private final Logger LOGGER = LoggerFactory.getLogger(GitStatusTest.class);

    @BeforeEach
    void setUp(@TempDir Path temporaryRepository) throws GitAPIException, GitException {
        git = Git.init().setDirectory(temporaryRepository.toFile()).call();
        repositoryPath = temporaryRepository;
        gitStatus = new GitStatus(git);
    }

    @AfterEach
    void tearDown() throws IOException {
        git.close();
    }


    @Test
    void hasUntrackedFiles_NoUntrackedFiles() throws GitAPIException, GitException {
        assertFalse(gitStatus.hasUntrackedFiles(), "Expected no untracked files");
    }

    @Test
    void hasUntrackedFiles_WithUntrackedFiles() throws IOException, GitAPIException, GitException {
        Path newFile = Files.createFile(repositoryPath.resolve("untracked.txt"));
        assertTrue(gitStatus.hasUntrackedFiles(), "Expected untracked files");
    }


    @Test
    void getUntrackedFiles_NoUntrackedFiles() throws GitAPIException, GitException {
        Set<String> untrackedFiles = gitStatus.getUntrackedFiles();
        assertTrue(untrackedFiles.isEmpty(), "Expected no untracked files");
    }

    @Test
    void getUntrackedFiles_WithUntrackedFiles() throws IOException, GitAPIException, GitException {
        Path newFile = Files.createFile(repositoryPath.resolve("untracked.txt"));
        Set<String> untrackedFiles = gitStatus.getUntrackedFiles();
        assertTrue(untrackedFiles.contains("untracked.txt"), "Expected 'untracked.txt' in untracked files");
    }


    @Test
    void hasTrackedFiles_NoTrackedFiles() throws GitAPIException, GitException {
        assertFalse(gitStatus.hasTrackedFiles(), "Expected no tracked files");
    }

    @Test
    void hasTrackedFiles_WithTrackedFiles() throws IOException, GitAPIException, GitException {
        Path trackedFile = Files.createFile(repositoryPath.resolve("tracked.txt"));
        git.add().addFilepattern("tracked.txt").call();
        assertTrue(gitStatus.hasTrackedFiles(), "Expected tracked files");
    }


    @Test
    void getTrackedFiles_NoTrackedFiles() throws GitAPIException, GitException {
        Set<String> trackedFiles = gitStatus.getTrackedFiles();
        assertTrue(trackedFiles.isEmpty(), "Expected no tracked files");
    }

    @Test
    void getTrackedFiles_WithTrackedFiles() throws IOException, GitAPIException, GitException {
        Path trackedFile = Files.createFile(repositoryPath.resolve("tracked.txt"));
        git.add().addFilepattern("tracked.txt").call();
        Set<String> trackedFiles = gitStatus.getTrackedFiles();
        assertTrue(trackedFiles.contains("tracked.txt"), "Expected 'tracked.txt' in tracked files");
    }


    @Test
    void getBranchNames_SingleBranch() throws GitAPIException, GitException, IOException {
        Path dummyFile = Files.createFile(repositoryPath.resolve("dummy.txt"));
        git.add().addFilepattern("tracked.txt").call();
        git.commit().setMessage("Dummy commit").call();

        List<String> branchNames = gitStatus.getBranchNames();
        assertEquals(1, branchNames.size(), "Expected one branch");
        assertTrue(branchNames.contains("refs/heads/master"), "Expected 'refs/heads/master' branch");
    }

    @Test
    void getBranchNames_MultipleBranches() throws GitAPIException, GitException, IOException {
        Path dummyFileBranch1 = Files.createFile(repositoryPath.resolve("dummy1.txt"));
        Path dummyFileBranch2 = Files.createFile(repositoryPath.resolve("dummy2.txt"));


        git.add().addFilepattern("dummy1.txt").addFilepattern("tracked.txt").call();
        git.commit().setMessage("Dummy commit").call();

        git.checkout().setCreateBranch(true).setName("develop").call();

        git.add().addFilepattern("dummy2.txt").addFilepattern("tracked.txt").call();
        git.commit().setMessage("Dummy commit").call();

        List<String> branchNames = gitStatus.getBranchNames();
        assertEquals(2, branchNames.size(), "Expected two branches");
        assertTrue(branchNames.contains("refs/heads/master"), "Expected 'refs/heads/master' branch");
        assertTrue(branchNames.contains("refs/heads/develop"), "Expected 'refs/heads/develop' branch");
    }



    private void listRepoContents(String repoPath) {
        try (Git git = Git.open(new File(repoPath))) {
            Repository repository = git.getRepository();

            ObjectId headId = repository.resolve("HEAD");
            if (headId == null) {
                System.out.println("Repository is empty or has no HEAD commit.");
                return;
            }

            try (org.eclipse.jgit.revwalk.RevWalk revWalk = new org.eclipse.jgit.revwalk.RevWalk(repository)) {
                RevCommit headCommit = revWalk.parseCommit(headId);

                try (org.eclipse.jgit.treewalk.TreeWalk treeWalk = new org.eclipse.jgit.treewalk.TreeWalk(repository)) {
                    treeWalk.addTree(headCommit.getTree());
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(org.eclipse.jgit.treewalk.filter.TreeFilter.ALL);

                    System.out.println("Contents of the repository:");
                    while (treeWalk.next()) {
                        System.out.println(treeWalk.getPathString());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading repository: " + e.getMessage());
        }
    }
}
