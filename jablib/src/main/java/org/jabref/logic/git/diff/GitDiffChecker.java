package org.jabref.logic.git.diff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitDiffChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitDiffChecker.class);

    public static BibDatabaseContext checkDiffAgainstLastCommit(
            GitHandler gitHandler, Path filepath,
            ImportFormatPreferences importFormatPreferences,
            FileUpdateMonitor fileUpdateMonitor) throws IOException, JabRefException {
        try (Git git = Git.open(gitHandler.getRepositoryPathAsFile())) {
            Repository repository = git.getRepository();
            String relativePath = filepath.toString().replace('\\', '/');
            Optional<byte[]> headBytes = readBlobBytesFromHead(repository, relativePath);
            if (headBytes.isEmpty()) {
                return BibDatabaseContext.empty();
            }

            Path tempFile = Files.createTempFile("git-diff-", ".bib");
            try {
                Files.write(tempFile, headBytes.get());
                BibtexImporter importer = new BibtexImporter(importFormatPreferences, fileUpdateMonitor);
                return importer.importDatabase(tempFile).getDatabaseContext();
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    public static BibDatabaseContext checkSavedWorkingTreeVersion(
            Path savedFilePath,
            ImportFormatPreferences importFormatPreferences,
            FileUpdateMonitor fileUpdateMonitor
    ) throws IOException {
        if (!Files.exists(savedFilePath)) {
            return BibDatabaseContext.empty();
        }
        BibtexImporter importer = new BibtexImporter(importFormatPreferences, fileUpdateMonitor);
        return importer.importDatabase(savedFilePath).getDatabaseContext();
    }

    private static Optional<byte[]> readBlobBytesFromHead(Repository repository, String relativePath) throws IOException {
        ObjectId headId = repository.resolve("HEAD");
        if (headId == null) {
            LOGGER.debug("No head found so treating {} as newly created", relativePath);
            return Optional.empty();
        }

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit revCommit = revWalk.parseCommit(headId);
            try (TreeWalk treeWalk = TreeWalk.forPath(repository, relativePath, revCommit.getTree())) {
                if (treeWalk == null) {
                    LOGGER.debug("{} didi not exist as head, so it is newly added", relativePath);
                    return Optional.empty();
                }
                ObjectId blobId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(blobId);
                return Optional.of(loader.getBytes());
            }
        }
    }
}
