package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitActionExecutorTest {

    private Path repositoryPath;
    private GitManager gitManager;
    private GitActionExecutor gitActionExecutor;

    private final Logger LOGGER = LoggerFactory.getLogger(GitActionExecutorTest.class);

    @BeforeEach
    void setUp(@TempDir Path temporaryRepository) throws GitException {
        this.repositoryPath = temporaryRepository;
        this.gitManager = GitManager.initGitRepository(repositoryPath);
        this.gitActionExecutor = this.gitManager.getGitActionExecutor();
    }

    @AfterEach
    void close() {
        gitManager.close();
    }

    @Test
    void addAddOneFile() throws IOException, GitAPIException, GitException {
        Status status0 = gitActionExecutor.getGit().status().call();

        Path pathToTempFile = Files.createTempFile(repositoryPath, null, null);

        Status status0_5 = gitActionExecutor.getGit().status().call();

        gitActionExecutor.add(pathToTempFile);

        Status status = gitActionExecutor.getGit().status().call();
        String fileName = pathToTempFile.getFileName().toString();

        Set<String> addedFiles = status.getAdded();
        assertTrue(status.getAdded().contains(fileName));

        boolean isDeleted = pathToTempFile.toFile().delete();
        LOGGER.info("Temp file for test 1: {}", isDeleted);
    }

    @Test
    void addAddMultipleFiles() throws IOException, GitAPIException, GitException {
        List<Path> listOfPaths = new ArrayList<>();
        int numberOfFiles = 10;
        for (int i = 0; i < numberOfFiles; i++) {
            listOfPaths.add(Files.createTempFile(repositoryPath, "tmpNumber_" + i + "_", null));
        }
        gitActionExecutor.add(listOfPaths);

        Status status = gitActionExecutor.getGit().status().call();
        Set<String> addedFiles = status.getAdded();

        for (Path p : listOfPaths) {
            String f = p.getFileName().toString();
            assertTrue(addedFiles.contains(f));
        }

        for (Path p : listOfPaths) {
            p.toFile().delete();
        }
    }

    // don't really understand append completely, testing with false
    @Test
    void commitSingleFile() throws IOException, GitAPIException, GitException {
        Path pathToTempFile = Files.createTempFile(repositoryPath, null, null);
        gitActionExecutor.add(pathToTempFile);

        String commitMessage = "Test single file commit";
        gitActionExecutor.commit(commitMessage, false);

        Status statusAfterCommit = gitActionExecutor.getGit().status().call();
        assertTrue(statusAfterCommit.isClean());

        String fileName = pathToTempFile.getFileName().toString();
        // file should not be in staging area
        assertFalse(statusAfterCommit.getAdded().contains(fileName));

        Iterable<RevCommit> log = gitActionExecutor.getGit().log().setMaxCount(1).call();
        // check in the logs that the last commit message is overlapping
        String latestCommitMessage = log.iterator().next().getFullMessage();
        assertEquals(commitMessage, latestCommitMessage);
    }

    @Test
    void commitMultipleFiles() throws IOException, GitAPIException, GitException {
        List<Path> listOfPaths = new ArrayList<>();
        int numberOfFiles = 10;
        for (int i = 0; i < numberOfFiles; i++) {
            listOfPaths.add(Files.createTempFile(repositoryPath, "tmpNumber_" + i + "_", null));
        }
        gitActionExecutor.add(listOfPaths);

        String commitMessage = "Test multiple files commit";
        gitActionExecutor.commit(commitMessage, false);

        Status statusAfterCommit = gitActionExecutor.getGit().status().call();
        assertTrue(statusAfterCommit.isClean());

        // same as above, stage should not contain commited files
        Set<String> addedFiles = statusAfterCommit.getAdded();

        for (Path p : listOfPaths) {
            String f = p.getFileName().toString();
            assertFalse(addedFiles.contains(f));
        }

        Iterable<RevCommit> log = gitActionExecutor.getGit().log().setMaxCount(1).call();
        String latestCommitMessage = log.iterator().next().getFullMessage();
        assertEquals(commitMessage, latestCommitMessage);
    }


    // following amer;s test idea: create two directories, remote and local. add, commit to local, push to remote
    // verify
    @Test
    void pushWithoutSpecifyingMainAndBranch() throws IOException, GitAPIException {
        Path remoteRepoPath = Files.createTempDirectory("remote-repo");
        try (Repository remoteRepo = FileRepositoryBuilder.create(new File(remoteRepoPath.toFile(), ".git"))) {
            remoteRepo.create();

            URI remoteRepoURI = remoteRepoPath.toUri();
            URIish remoteRepoURIish = new URIish(remoteRepoURI.toString());
            gitActionExecutor.getGit().remoteAdd().setName("origin").setUri(remoteRepoURIish).call();

            List<Path> listOfPaths = new ArrayList<>();
            int numberOfFiles = 10;
            for (int i = 0; i < numberOfFiles; i++) {
                listOfPaths.add(Files.createTempFile(repositoryPath, "tmpNumber_" + i + "_", null));
            }
            gitActionExecutor.add(listOfPaths);

            String commitMessage = "Test multiple files push";
            gitActionExecutor.commit(commitMessage, false);

            gitActionExecutor.push("origin", "main");
            Status status = gitActionExecutor.getGit().status().call();

            try (Git remoteGit = Git.open(remoteRepoPath.toFile())) {
                boolean commitExists = remoteGit.log()
                                                .all()
                                                .call()
                                                .iterator()
                                                .hasNext();

                assertTrue(commitExists);

                Iterable<RevCommit> commits = remoteGit.log().all().call();
                LOGGER.info("-----------------------------------");

                String latestCommitMessageAccessedFromRemote = "";
                for (RevCommit commit : commits) {
                    LOGGER.info("Commit: {}", commit.getName());
                    LOGGER.info("Author: {}", commit.getAuthorIdent().getName());
                    LOGGER.info("Date: {}", commit.getAuthorIdent().getWhen());
                    LOGGER.info("Message: {}", commit.getFullMessage());
                    latestCommitMessageAccessedFromRemote = commit.getFullMessage();

                    LOGGER.info("-----------------------------------");
                }

                assertEquals(commitMessage, latestCommitMessageAccessedFromRemote);
            }
        } catch (URISyntaxException | GitException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void pushWithoutSettingOrigin(@TempDir Path tempPath) throws GitException, IOException {
        GitManager gitManager = GitManager.initGitRepository(tempPath);
        GitActionExecutor actionExecutor = gitManager.getGitActionExecutor();
        Path tempFile = Files.createTempFile(tempPath, "test", null);
        actionExecutor.add(tempFile);
        actionExecutor.commit("test", false);
        GitException exception = assertThrows(GitException.class, actionExecutor::push);
        assertEquals("Push failed", exception.getMessage());
        assertEquals(TransportException.class, exception.getCause().getClass());
        assertEquals("origin: not found.", exception.getCause().getMessage());
    }


    @Test
    void pullWithoutRebase() throws IOException, GitException, GitAPIException, URISyntaxException {
        Path remoteRepoPath = Files.createTempDirectory("remote-repo");
        try (Repository remoteRepo = FileRepositoryBuilder.create(new File(remoteRepoPath.toFile(), ".git"))) {
            remoteRepo.create();

            URI remoteRepoURI = remoteRepoPath.toUri();
            URIish remoteRepoURIish = new URIish(remoteRepoURI.toString());

            String remoteFileName = "";
            try (Git remoteGit = Git.open(remoteRepoPath.toFile())) {
                Path remoteFile = Files.createTempFile(remoteRepoPath, "remote-file", ".txt");
                remoteFileName = remoteFile.getFileName().toString();
                Files.writeString(remoteFile, "Remote content");
                remoteGit.add().addFilepattern(remoteFile.getFileName().toString()).call();
                remoteGit.commit().setMessage("Initial commit in remote").call();

                remoteGit.checkout().setCreateBranch(true).setName("main").call();

                remoteGit.push();
            }

            gitActionExecutor.getGit().remoteAdd().setName("origin").setUri(remoteRepoURIish).call();
            gitActionExecutor.pull(false, "origin", "main");

            listRepoContents(repositoryPath.toString());

            Path pulledFile = repositoryPath.resolve(remoteFileName);
            assertTrue(pulledFile.toFile().exists(), "Pulled file does not exist in local repository");
            assertEquals("Remote content", Files.readString(pulledFile), "Pulled file content does not match remote content");
        }
    }

    @Test
    void pullWithRebase() throws IOException, GitException, GitAPIException, URISyntaxException {
        Path remoteRepoPath = Files.createTempDirectory("remote-repo");

        try (Repository remoteRepo = FileRepositoryBuilder.create(new File(remoteRepoPath.toFile(), ".git"))) {
            remoteRepo.create();

            URI remoteRepoURI = remoteRepoPath.toUri();
            URIish remoteRepoURIish = new URIish(remoteRepoURI.toString());
            gitActionExecutor.getGit().remoteAdd().setName("origin").setUri(remoteRepoURIish).call();


            String A = "";
            String C = "";
            try (Git remoteGit = Git.open(remoteRepoPath.toFile())) {
                Path remoteFile = Files.createTempFile(remoteRepoPath, "A", ".txt");
                Files.writeString(remoteFile, "Remote/Local content v1");

                A = remoteFile.getFileName().toString();

                remoteGit.add().addFilepattern(A).call();
                remoteGit.commit().setMessage("Initial commit in remote").call();

                remoteGit.checkout().setCreateBranch(true).setName("main").call();
                remoteGit.push();
            }
            //replicated commits from remote
            gitActionExecutor.pull(false, "origin", "main");


            //update remote repository
            try (Git remoteGit = Git.open(remoteRepoPath.toFile())) {
                Path remoteFile2 = Files.createTempFile(remoteRepoPath, "C", ".txt");
                Files.writeString(remoteFile2, "Remote content v2");

                C = remoteFile2.getFileName().toString();
                remoteGit.add().addFilepattern(C).call();
                remoteGit.commit().setMessage("Second commit in remote").call();
                remoteGit.push();
            }

            String B = "";

            Path localFile2 = Files.createTempFile(repositoryPath, "B", ".txt");
            Files.writeString(localFile2, "Local content v2");
            B = localFile2.getFileName().toString();

            gitActionExecutor.add(localFile2);
            gitActionExecutor.commit("Second commit to local repo", false);
            gitActionExecutor.push();



            gitActionExecutor.pull(true, "origin", "main");

            listRepoContents(repositoryPath.toString());

            Path pulledFile1 = repositoryPath.resolve(C);
            Path pulledFile2 = repositoryPath.resolve(B);

            assertTrue(pulledFile1.toFile().exists(), "C not found after pull with rebase");
            assertTrue(pulledFile2.toFile().exists(), "B not found after pull with rebase");

            // local commit should be rebased on top of the remote commit
            assertEquals("Remote content v2", Files.readString(pulledFile1), "Content mismatch in C");
            assertEquals("Local content v2", Files.readString(pulledFile2), "Content mismatch in B");
        }
    }


    @Test
    void testUndoPull() throws IOException, GitException, GitAPIException, URISyntaxException {

        Path remoteRepoPath = Files.createTempDirectory("remote-repo");
        try (Repository remoteRepo = FileRepositoryBuilder.create(new File(remoteRepoPath.toFile(), ".git"))) {
            remoteRepo.create();
        }
//        GitManager remoteGitManager = GitManager.initGitRepository(remoteRepoPath);

        URI remoteRepoURI = remoteRepoPath.toUri();
        URIish remoteRepoURIish = new URIish(remoteRepoURI.toString());
        gitActionExecutor.getGit().remoteAdd().setName("origin").setUri(remoteRepoURIish).call();
        Path remoteMergeConflictFile = Files.createTempFile(remoteRepoPath, "merge-conflict", ".txt");
        Files.writeString(remoteMergeConflictFile, "merge conflict - remote");
        Path remoteFile = Files.createTempFile(remoteRepoPath, "remote-file", ".txt");
        Files.writeString(remoteFile, "Remote content");

        try (Git remoteGit = Git.open(remoteRepoPath.toFile())) {

            remoteGit.add().addFilepattern(remoteFile.getFileName().toString()).call();
            remoteGit.add().addFilepattern(remoteMergeConflictFile.getFileName().toString()).call();
            remoteGit.commit().setMessage("Initial commit in remote").call();
            remoteGit.checkout().setCreateBranch(true).setName("main").call();
            remoteGit.push();
        }

        Path untrackedLocalFile = Files.createTempFile(repositoryPath, "local-file", ".txt");
        Files.writeString(untrackedLocalFile, "untracked Local file content");
        Path localMergeConflictFile = Files.createFile(repositoryPath.resolve(remoteMergeConflictFile.getFileName().toString()));
        Files.writeString(localMergeConflictFile, "merge conflict - local");
        gitActionExecutor.add(localMergeConflictFile);
        gitActionExecutor.commit("Add local-merge-conflict.txt", false);



        System.out.println("-------------------------------------------------");
        System.out.println(Arrays.toString(remoteRepoPath.toFile().listFiles()));
        System.out.println("-------------------------------------------------");
        System.out.println(Arrays.toString(repositoryPath.toFile().listFiles()));
        System.out.println("-------------------------------------------------");

//        TODO: remove this after debugging
        gitActionExecutor.pull(true, "origin", "main");
        System.out.println("-------------------------------------------------");
        System.out.println(Arrays.toString(remoteRepoPath.toFile().listFiles()));
        System.out.println("-------------------------------------------------");
        System.out.println(Arrays.toString(repositoryPath.toFile().listFiles()));
        System.out.println("-------------------------------------------------");
        System.out.println(Files.readString(localMergeConflictFile));
        System.out.println(Files.readString(remoteMergeConflictFile));

        Iterable<RevCommit> commits = gitManager.getGitActionExecutor().getGit().log().all().call();
        LOGGER.info("-----------------------------------");

        for (RevCommit commit : commits) {
            LOGGER.info("Message: {}", commit.getFullMessage());

            LOGGER.info("-----------------------------------");
        }
//        TODO: pull should throw an exception in case of a merge conflict
//        GitException exception = assertThrows(GitException.class,
//                () -> gitActionExecutor.pull(true, "origin", "main"));
//        assertEquals("Pull failed", exception.getMessage());
//        assertEquals(TransportException.class, exception.getCause().getClass());
//        assertEquals("origin: not found.", exception.getCause().getMessage());

        assertTrue(Files.exists(repositoryPath.resolve(remoteFile.getFileName().toString())),
                "remote-file should exist in local repository after failed pull");

        assertTrue(Files.exists(untrackedLocalFile));
        gitActionExecutor.undoPull();
        assertFalse(Files.exists(repositoryPath.resolve(remoteFile.getFileName().toString())),
                "remote-file should not exist in local repository after undoing pull");
        assertTrue(Files.exists(untrackedLocalFile), "Local file should still exist after undoing pull");

        assertEquals("merge conflict - local", Files.readString(localMergeConflictFile),
                "Local file content should remain unchanged");

        // TODO: remove after debugging
        System.out.println("-------------------------------------------------");
        System.out.println(Arrays.toString(remoteRepoPath.toFile().listFiles()));
        System.out.println("-------------------------------------------------");
        System.out.println(Arrays.toString(repositoryPath.toFile().listFiles()));
        System.out.println("-------------------------------------------------");
        System.out.println(Files.readString(localMergeConflictFile));
        System.out.println(Files.readString(remoteMergeConflictFile));

        commits = gitManager.getGitActionExecutor().getGit().log().all().call();
        LOGGER.info("-----------------------------------");
        for (RevCommit commit : commits) {
            LOGGER.info("Message: {}", commit.getFullMessage());

            LOGGER.info("-----------------------------------");
        }
    }


    /**
     * Test stashing unstaged changes.
     * Check that modifications not added to the staging area are stashed.
     */
    @Test
    void stashUnstagedChanges() throws IOException, GitException, GitAPIException {
        // comiting random file so that head is not null
        Path initialFile = Files.createFile(repositoryPath.resolve("initial.txt"));
        Files.writeString(initialFile, "Initial commit");
        gitActionExecutor.add(initialFile);
        gitActionExecutor.commit("Initial commit", false);

        // check that the working directory is clean
        Status statusBeforeStash = gitActionExecutor.getGit().status().call();
        assertTrue(statusBeforeStash.isClean(), "Repository should have changes before stashing");

        gitActionExecutor.stash();

        Status statusAfterStash = gitActionExecutor.getGit().status().call();
        assertTrue(statusAfterStash.isClean(), "Repository should be clean after stashing");

        gitActionExecutor.applyLatestStash();

        // check that the staged changes are reapplied
        Status statusAfterApply = gitActionExecutor.getGit().status().call();
        assertFalse(statusAfterApply.isClean(), "Repository should have unstaged changes after applying stash");
        assertTrue(Files.exists(initialFile), "Stashed file should exist after applying stash");
        assertEquals("Unstaged changes", Files.readString(initialFile), "File content should match stashed changes");
    }



    private void listRepoContents(String repoPath) {
        try (Git git = Git.open(new File(repoPath))) {
            Repository repository = git.getRepository();

            ObjectId headId = repository.resolve("HEAD");
            if (headId == null) {
                System.out.println("Repository is empty or has no HEAD commit.");
                return;
            }

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit headCommit = revWalk.parseCommit(headId);

                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(headCommit.getTree());
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(TreeFilter.ALL);

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

    void deleteDirWithContent(Path path) throws IOException {
        Files.walk(path)
             .sorted(Comparator.reverseOrder())
             .map(Path::toFile)
             .forEach(File::delete);
    }
}

