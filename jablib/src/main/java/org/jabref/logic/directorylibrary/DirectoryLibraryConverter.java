package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.exporter.HayagrivaEntryWriter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NullMarked;

/// Converts a regular `.bib` library into a directory library: every entry gets a Markdown
/// sidecar (see [MarkdownSidecar]) next to its linked file, and the `.bib` itself becomes the
/// library's mirror. The conversion is only offered when the whole library fits under one
/// root — [#obstacles] lists everything that prevents it.
// [impl->req~directory-library.convert~1]
@NullMarked
public class DirectoryLibraryConverter {

    private final MarkdownSidecar markdownSidecar = new MarkdownSidecar();

    /// The directory that becomes the library root: the library-specific file directory when
    /// one is configured, otherwise the `.bib` file's directory.
    public static Optional<Path> determineRoot(BibDatabaseContext context) {
        Optional<Path> bibDirectory = context.getDatabasePath().map(Path::getParent);
        return context.getMetaData().getLibrarySpecificFileDirectory()
                      .map(Path::of)
                      .map(directory -> directory.isAbsolute() || bibDirectory.isEmpty()
                                        ? directory
                                        : bibDirectory.get().resolve(directory))
                      .map(Path::normalize)
                      .or(() -> bibDirectory);
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
                Optional<Path> resolved = linkedFile.findIn(fileDirectories);
                if (resolved.isEmpty()) {
                    obstacles.add(Localization.lang("Linked file '%0' of entry '%1' was not found.", linkedFile.getLink(), label));
                } else if (!resolved.get().toAbsolutePath().normalize().startsWith(normalizedRoot)) {
                    obstacles.add(Localization.lang("Linked file '%0' of entry '%1' is outside of '%2'.", linkedFile.getLink(), label, root.toString()));
                }
            }
        }
        return obstacles;
    }

    /// Writes one single-entry Markdown sidecar per entry: next to the entry's first linked
    /// file (sharing its base name, per the pairing convention), or named after the citation
    /// key in the root. Occupied names are uniquified with a numeric suffix.
    public void writeSidecars(BibDatabaseContext context, Path root, FilePreferences filePreferences) throws IOException {
        List<Path> fileDirectories = context.getFileDirectories(filePreferences);
        for (BibEntry entry : context.getDatabase().getEntries()) {
            Path sidecar = sidecarFor(entry, root, fileDirectories);
            String key = entry.getCitationKey().filter(citationKey -> !citationKey.isBlank()).orElse("entry");
            String document = markdownSidecar.merge(null, List.of(new HayagrivaEntryWriter.KeyedEntry("", key, entry)));
            Files.writeString(sidecar, document);
        }
    }

    private static Path sidecarFor(BibEntry entry, Path root, List<Path> fileDirectories) {
        Optional<Path> pairedFile = entry.getFiles().stream()
                                         .filter(linkedFile -> !linkedFile.isOnlineLink())
                                         .findFirst()
                                         .flatMap(linkedFile -> linkedFile.findIn(fileDirectories));
        Path directory;
        String baseName;
        if (pairedFile.isPresent()) {
            directory = pairedFile.get().getParent();
            baseName = FileUtil.getBaseName(pairedFile.get());
        } else {
            directory = root;
            baseName = entry.getCitationKey().filter(key -> !key.isBlank()).orElse("entry");
        }
        Path sidecar = directory.resolve(baseName + "." + MarkdownSidecar.MARKDOWN_EXTENSION);
        int counter = 1;
        while (Files.exists(sidecar)) {
            sidecar = directory.resolve(baseName + "-" + counter++ + "." + MarkdownSidecar.MARKDOWN_EXTENSION);
        }
        return sidecar;
    }
}
