package org.jabref.logic.git.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

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

public class GitFileReader {
    public static String readFileFromCommit(Git git, RevCommit commit, Path filePath) throws JabRefException {
        // 1. get commit-pointing tree
        Repository repository = git.getRepository();
        RevTree commitTree = commit.getTree();

        // 2. setup TreeWalk + to the target file
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, String.valueOf(filePath), commitTree)) {
            if (treeWalk == null) {
                throw new JabRefException("File '" + filePath + "' not found in commit " + commit.getName());
            }
            // 3. load blob object
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);
            return new String(loader.getBytes(), StandardCharsets.UTF_8);
        } catch (MissingObjectException |
                 IncorrectObjectTypeException e) {
            throw new JabRefException("Git object missing or incorrect when reading file: " + filePath, e);
        } catch (IOException e) {
            throw new JabRefException("I/O error while reading file from commit: " + filePath, e);
        }
    }
}
