package org.jabref.logic.git.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitRevisionLocatorTest {
    @Test
    void testLocateMergeCommits(@TempDir Path tempDir) throws Exception {
        Path bibFile = tempDir.resolve("library.bib");
        Git git = Git.init().setDirectory(tempDir.toFile()).setInitialBranch("main").call();

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

        // simulate fake remote ref
        git.getRepository().updateRef("refs/remotes/origin/main").link("refs/heads/remote");

        // test locator
        GitRevisionLocator locator = new GitRevisionLocator();
        RevisionTriple triple = locator.locateMergeCommits(git);

        assertEquals(base.getId(), triple.base().getId());
        assertEquals(local.getId(), triple.local().getId());
        assertEquals(remote.getId(), triple.remote().getId());
    }
}
