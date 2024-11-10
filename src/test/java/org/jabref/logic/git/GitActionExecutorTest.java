


package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void testAdd_addOneFile() throws IOException, GitAPIException, GitException {
        Status status0 = gitActionExecutor.getGit().status().call();

        Path pathToTempFile = Files.createTempFile(repositoryPath, null, null);

        Status status0_5 = gitActionExecutor.getGit().status().call();

        gitActionExecutor.add(pathToTempFile);

        Status status = gitActionExecutor.getGit().status().call();
        String fileName = pathToTempFile.getFileName().toString();

        Set<String> addedFiles = status.getAdded();
        assertTrue(status.getAdded().contains(fileName));

        boolean isDeleted = pathToTempFile.toFile().delete();
        LOGGER.info("Temp file for test 1: " + isDeleted);
    }

    @Test
    void testAdd_addMultipleFiles() throws IOException, GitAPIException, GitException {
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
    void testCommit_singleFile() throws IOException, GitAPIException, GitException {
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
    void testCommit_multipleFiles() throws IOException, GitAPIException, GitException {
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
    void testPush_withoutSpecifyingMainAndBranch() throws IOException, GitAPIException {
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
                    LOGGER.info("Commit: " + commit.getName());
                    LOGGER.info("Author: " + commit.getAuthorIdent().getName());
                    LOGGER.info("Date: " + commit.getAuthorIdent().getWhen());
                    LOGGER.info("Message: " + commit.getFullMessage());
                    latestCommitMessageAccessedFromRemote = commit.getFullMessage();

                    LOGGER.info("-----------------------------------");
                }

                assertEquals(commitMessage, latestCommitMessageAccessedFromRemote);
            }
        } catch (URISyntaxException | GitException e) {
            throw new RuntimeException(e);
        }
    }
}

