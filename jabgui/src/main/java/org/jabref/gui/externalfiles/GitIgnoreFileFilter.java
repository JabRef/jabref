package org.jabref.gui.externalfiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.jabref.logic.os.OS;

import org.eclipse.jgit.ignore.IgnoreNode;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [impl->req~jabgui.externalfiles.unlinked-files.search.gitignore-precedence~1]
// [impl->req~jabgui.externalfiles.unlinked-files.search.gitignore-windows-case~1]
// [impl->req~jabgui.externalfiles.unlinked-files.search.gitignore-symlink~1]
public class GitIgnoreFileFilter implements DirectoryStream.Filter<Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitIgnoreFileFilter.class);

    private static final String DEFAULT_IGNORES = """
            .gitignore
            .git
            .DS_Store
            desktop.ini
            Thumbs.db
            """;

    private final List<Map.Entry<Path, IgnoreNode>> ignoreNodes = new ArrayList<>();

    public GitIgnoreFileFilter(Path path) {
        IgnoreNode ignoreNode = new IgnoreNode();

        // Always ignore ".gitignore" itself and common defaults.
        // Parsed first, because the last matching rule wins: negations in the user's .gitignore can override these.
        String defaults = OS.WINDOWS ? DEFAULT_IGNORES.toLowerCase(Locale.ROOT) : DEFAULT_IGNORES;
        try (InputStream input = new ByteArrayInputStream(defaults.getBytes(StandardCharsets.UTF_8))) {
            ignoreNode.parse(input);
        } catch (IOException e) {
            LOGGER.info("Could not add default ignores", e);
        }

        Path directory = path.toAbsolutePath().normalize();
        ignoreNodes.add(Map.entry(directory, ignoreNode));
        List<Path> ancestors = new ArrayList<>();
        for (Path current = directory; current != null; current = current.getParent()) {
            ancestors.add(current);
        }
        for (Path ancestor : ancestors.reversed()) {
            addIgnoreFile(ancestor);
        }
    }

    private GitIgnoreFileFilter(Path path, GitIgnoreFileFilter parent) {
        Path directory = path.toAbsolutePath().normalize();
        ignoreNodes.add(Map.entry(directory, parent.ignoreNodes.getFirst().getValue()));
        ignoreNodes.addAll(parent.ignoreNodes.subList(1, parent.ignoreNodes.size()));
        addIgnoreFile(directory);
    }

    GitIgnoreFileFilter forSubdirectory(Path path) {
        return new GitIgnoreFileFilter(path, this);
    }

    private void addIgnoreFile(Path ancestor) {
        Path gitIgnore = ancestor.resolve(".gitignore");
        if (Files.isRegularFile(gitIgnore)) {
            IgnoreNode node = new IgnoreNode();
            try {
                String rules = Files.readString(gitIgnore);
                if (OS.WINDOWS) {
                    rules = rules.toLowerCase(Locale.ROOT);
                }
                try (InputStream input = new ByteArrayInputStream(rules.getBytes(StandardCharsets.UTF_8))) {
                    node.parse(input);
                }
                ignoreNodes.add(Map.entry(ancestor, node));
            } catch (IOException e) {
                LOGGER.info("Could not read .gitignore from {}", gitIgnore, e);
            }
        }
    }

    @Override
    public boolean accept(Path path) throws IOException {
        return accept(path, Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS));
    }

    boolean accept(Path path, boolean isDirectory) {
        boolean ignored = false;
        Path absolutePath = path.toAbsolutePath().normalize();
        for (Map.Entry<Path, IgnoreNode> entry : ignoreNodes) {
            Path relative = safeRelativize(entry.getKey(), absolutePath);
            String relativeStr = StreamSupport.stream(relative.spliterator(), false).map(Path::toString).collect(Collectors.joining("/"));
            if (relativeStr.isEmpty()) {
                continue;
            }
            if (OS.WINDOWS) {
                relativeStr = relativeStr.toLowerCase(Locale.ROOT);
            }
            @Nullable Boolean match = entry.getValue().checkIgnored(relativeStr, isDirectory);
            if (match != null) {
                ignored = match;
            }
        }
        return !ignored;
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
