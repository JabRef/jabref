package org.jabref.logic.git.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.JabRefException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class GitFileReader {
    public static Optional<String> readFileFromCommit(Git git, RevCommit commit, Path relativePath) throws JabRefException {
        // ref: https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/api/ReadFileFromCommit.java
        // 1. get commit-pointing tree
        Repository repository = git.getRepository();
        RevTree commitTree = commit.getTree();

        // 2. setup TreeWalk + to the target file
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, relativePath.toString(), commitTree)) {
            if (treeWalk == null) {
                return Optional.empty();
            }
            // 3. load blob object
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);
            return Optional.of(new String(loader.getBytes(), StandardCharsets.UTF_8));
        } catch (MissingObjectException | IncorrectObjectTypeException e) {
            throw new JabRefException("Git object missing or incorrect when reading file: " + relativePath, e);
        } catch (IOException e) {
            throw new JabRefException("I/O error while reading file from commit: " + relativePath, e);
        }
    }
}
