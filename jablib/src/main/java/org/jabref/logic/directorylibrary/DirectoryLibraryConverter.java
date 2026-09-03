package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.exporter.HayagrivaEntryWriter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileNameCleaner;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.function.Predicate.not;

/// Converts a regular `.bib` library into a directory library: every entry gets a Markdown
/// sidecar (see [MarkdownSidecar]) next to its linked file, and the `.bib` itself becomes the
/// library's mirror. The conversion is only offered when the whole library fits under one
/// root — [#obstacles] lists everything that prevents it.
// [impl->req~directory-library.convert~1]
@NullMarked
public class DirectoryLibraryConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryLibraryConverter.class);

    private final MarkdownSidecar markdownSidecar = new MarkdownSidecar();

    /// The directory that becomes the library root: the library-specific file directory when
    /// one is configured, otherwise the `.bib` file's directory.
    public static Optional<Path> determineRoot(BibDatabaseContext context, FilePreferences filePreferences) {
        return context.getAllFileDirectories(filePreferences)
                      .getLibraryDirectoryOpt()
                      .or(context::getDatabaseDirectory)
                      .map(Path::normalize);
    }

    /// Everything that prevents the conversion: linked files that cannot be found or do not
    /// live under the root, and library content sidecars cannot represent (BibTeX strings,
    /// preamble). An empty result means the library converts losslessly file-wise.
    public List<String> obstacles(BibDatabaseContext context, Path root, FilePreferences filePreferences) {
        List<String> obstacles = new ArrayList<>();
        if (context.getDatabase().getPreamble().isPresent()) {
            obstacles.add(Localization.lang("The library contains a preamble, which a folder library cannot represent."));
        }
        if (!context.getDatabase().getStringValues().isEmpty()) {
            obstacles.add(Localization.lang("The library contains BibTeX strings, which a folder library cannot represent."));
        }
        List<Path> fileDirectories = context.getFileDirectories(filePreferences);
        Path normalizedRoot = root.toAbsolutePath().normalize();
        for (BibEntry entry : context.getDatabase().getEntries()) {
            String label = entry.getCitationKey().orElseGet(() -> entry.getAuthorTitleYear(40));
            for (LinkedFile linkedFile : entry.getFiles()) {
                if (linkedFile.isOnlineLink()) {
                    continue;
                }
                linkedFile.findIn(fileDirectories).ifPresentOrElse(resolved -> {
                    if (!resolved.toAbsolutePath().normalize().startsWith(normalizedRoot)) {
                        obstacles.add(Localization.lang("Linked file '%0' of entry '%1' is outside of '%2'.", linkedFile.getLink(), label, root.toString()));
                    }
                }, () -> obstacles.add(Localization.lang("Linked file '%0' of entry '%1' was not found.", linkedFile.getLink(), label)));
            }
        }
        return obstacles;
    }

    /// Runs once the library is saved: sidecars first, then the `.bib` moves into the root as
    /// the mirror, and a copy becomes the merge base — so reopening the root finds mirror and
    /// base identical instead of merging the library against itself.
    ///
    /// @return the mirror file
    public Path convert(BibDatabaseContext context, Path root, FilePreferences filePreferences) throws IOException {
        Path bibFile = context.getDatabasePath().orElseThrow();
        Path mirror = root.resolve(DirectoryLibrarySynchronizer.mirrorFileName(root));
        writeSidecars(context, root, filePreferences);
        if (!mirror.equals(bibFile)) {
            Files.move(bibFile, mirror);
        }
        Path base = DirectoryLibrarySynchronizer.mirrorBaseFile(root);
        Files.createDirectories(base.getParent());
        Files.copy(mirror, base, StandardCopyOption.REPLACE_EXISTING);
        return mirror;
    }

    /// Writes one single-entry Markdown sidecar per entry: next to the entry's first linked
    /// file (sharing its base name, per the pairing convention), or named after the citation
    /// key in the root. Occupied names are uniquified with a numeric suffix. A failure removes
    /// the sidecars written so far, so a retry does not produce duplicates.
    public void writeSidecars(BibDatabaseContext context, Path root, FilePreferences filePreferences) throws IOException {
        List<Path> fileDirectories = context.getFileDirectories(filePreferences);
        List<Path> written = new ArrayList<>();
        try {
            for (BibEntry entry : context.getDatabase().getEntries()) {
                Path sidecar = sidecarFor(entry, root, fileDirectories);
                String key = entry.getCitationKey().filter(not(String::isBlank)).orElse("entry");
                String document = markdownSidecar.merge("", List.of(new HayagrivaEntryWriter.KeyedEntry("", key, entry)));
                Files.writeString(sidecar, document);
                written.add(sidecar);
            }
        } catch (IOException e) {
            written.forEach(DirectoryLibraryConverter::deleteQuietly);
            throw e;
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.warn("Could not remove partially written sidecar {}", file, e);
        }
    }

    private static Path sidecarFor(BibEntry entry, Path root, List<Path> fileDirectories) {
        Path baseNamePath = entry.getFiles().stream()
                                 .filter(linkedFile -> !linkedFile.isOnlineLink())
                                 .findFirst()
                                 .flatMap(linkedFile -> linkedFile.findIn(fileDirectories))
                                 .map(paired -> paired.resolveSibling(FileUtil.getBaseName(paired)))
                                 .orElseGet(() -> root.resolve(entry.getCitationKey()
                                                                    .map(FileNameCleaner::cleanFileName)
                                                                    .filter(not(String::isBlank))
                                                                    .orElse("entry")));
        Path sidecar = baseNamePath.resolveSibling(baseNamePath.getFileName() + "." + MarkdownSidecar.MARKDOWN_EXTENSION);
        for (int counter = 1; Files.exists(sidecar); counter++) {
            sidecar = baseNamePath.resolveSibling(baseNamePath.getFileName() + "-" + counter + "." + MarkdownSidecar.MARKDOWN_EXTENSION);
        }
        return sidecar;
    }
}
