package org.jabref.gui.externalfiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.jgit.ignore.IgnoreNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitIgnoreFileFilter implements DirectoryStream.Filter<Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitIgnoreFileFilter.class);

    private static final String DEFAULT_IGNORES = ".gitignore\n.git\n.DS_Store\ndesktop.ini\nThumbs.db\n";

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
        } else {
            this.baseDir = currentPath;
            Path gitIgnore = currentPath.resolve(".gitignore");
            try (InputStream input = Files.newInputStream(gitIgnore)) {
                ignoreNode.parse(input);
            } catch (IOException e) {
                LOGGER.info("Could not read .gitignore from {}", gitIgnore, e);
            }
        }

        // Always ignore ".gitignore" itself and common defaults.
        // Parsed last, so the rules apply whether or not a .gitignore was found.
        try (InputStream input = new ByteArrayInputStream(DEFAULT_IGNORES.getBytes(StandardCharsets.UTF_8))) {
            ignoreNode.parse(input);
        } catch (IOException e) {
            LOGGER.info("Could not add default ignores", e);
        }
    }

    @Override
    public boolean accept(Path path) throws IOException {
        Path relative = safeRelativize(baseDir, path);
        // Build the path with '/' separators from the path's own components;
        // a plain string replacement would corrupt names legally containing a backslash
        String relativeStr = StreamSupport.stream(relative.spliterator(), false)
                                          .map(Path::toString)
                                          .collect(Collectors.joining("/"));
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
