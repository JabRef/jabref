package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.ignore.IgnoreNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitIgnoreFileFilter implements DirectoryStream.Filter<Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitIgnoreFileFilter.class);

    private final IgnoreNode ignoreNode;
    private final Path baseDir;

    public GitIgnoreFileFilter(Path path) {
        Path currentPath = path;
        while ((currentPath != null) && !Files.exists(currentPath.resolve(".gitignore"))) {
            currentPath = currentPath.getParent();
        }

        this.ignoreNode = new IgnoreNode();

        if (currentPath == null) {
            this.baseDir = path;
            // Add default ignores
            try (java.io.ByteArrayInputStream input = new java.io.ByteArrayInputStream(
                    ".git\n.DS_Store\ndesktop.ini\nThumbs.db\n.gitignore\n".getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                ignoreNode.parse(input);
            } catch (IOException e) {
                LOGGER.info("Could not parse default ignores", e);
            }
        } else {
            this.baseDir = currentPath;
            Path gitIgnore = currentPath.resolve(".gitignore");
            try (InputStream input = Files.newInputStream(gitIgnore)) {
                ignoreNode.parse(input);
            } catch (IOException e) {
                LOGGER.info("Could not read .gitignore from {}", gitIgnore, e);
            }
            // Always ignore .gitignore itself and common defaults
            try (java.io.ByteArrayInputStream input = new java.io.ByteArrayInputStream(
                    ".gitignore\n.git\n.DS_Store\ndesktop.ini\nThumbs.db\n".getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                ignoreNode.parse(input);
            } catch (IOException e) {
                LOGGER.info("Could not add default ignores", e);
            }
        }
    }

    @Override
    public boolean accept(Path path) throws IOException {
        Path relative = safeRelativize(baseDir, path);
        String relativeStr = relative.toString().replace('\\', '/');
        if (relativeStr.isEmpty()) {
            return true;
        }
        boolean isDirectory = Files.isDirectory(path);
        Boolean ignored = ignoreNode.checkIgnored(relativeStr, isDirectory);
        // checkIgnored returns Boolean.TRUE if ignored, Boolean.FALSE if explicitly not ignored, or null if no rule matched
        return ignored == null || !ignored;
    }

    private static Path safeRelativize(Path base, Path child) {
        try {
            return base.relativize(child);
        } catch (IllegalArgumentException _) {
            // If paths are on different roots, fall back to just the file name for relative matching
            return child.getFileName();
        }
    }
}
