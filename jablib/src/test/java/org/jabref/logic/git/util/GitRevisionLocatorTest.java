package org.jabref.logic.git.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.git.io.GitRevisionLocator;
import org.jabref.logic.git.io.RevisionTriple;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("git")
class GitRevisionLocatorTest {
    private Git git;

    @BeforeEach
    void setup() {
        SystemReader.setInstance(new NoopGitSystemReader());
    }

    @AfterEach
    void cleanup() {
        if (git != null) {
            git.close();
        }
    }

    @Test
    void locateMergeCommits(@TempDir Path tempDir) throws Exception {
        Path bibFile = tempDir.resolve("library.bib");
        git = Git.init().setDirectory(tempDir.toFile()).setInitialBranch("main").call();

        // create base commit
        Files.writeString(bibFile, "@article{a, author = {x}}", StandardCharsets.UTF_8);
        git.add().addFilepattern("library.bib").call();
        RevCommit base = git.commit().setMessage("base").call();

        // create local (HEAD)
        Files.writeString(bibFile, "@article{a, author = {local}}", StandardCharsets.UTF_8);
        git.add().addFilepattern("library.bib").call();
        RevCommit local = git.commit().setMessage("local").call();

        // create remote branch and commit
        git.checkout().setName("remote").setCreateBranch(true).setStartPoint(base).call();
        Files.writeString(bibFile, "@article{a, author = {remote}}", StandardCharsets.UTF_8);
        git.add().addFilepattern("library.bib").call();
        RevCommit remote = git.commit().setMessage("remote").call();

        // restore HEAD to local
        git.checkout().setName("main").call();

        git.remoteAdd()
           .setName("origin")
           .setUri(new URIish(tempDir.toUri().toString()))
           .call();
        git.getRepository().updateRef("refs/remotes/origin/main").link("refs/heads/remote");

        StoredConfig config = git.getRepository().getConfig();
        config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, "main", ConfigConstants.CONFIG_KEY_REMOTE, "origin");
        config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, "main", ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + "main");
        config.save();

        git.checkout().setName("main").call();

        // test locator
        GitRevisionLocator locator = new GitRevisionLocator();
        RevisionTriple triple = locator.locateMergeCommits(git);

        assertTrue(triple.base().isPresent());
        assertEquals(base.getId(), triple.base().get().getId());
        assertEquals(local.getId(), triple.local().getId());
        assertEquals(remote.getId(), triple.remote().getId());
    }
}
