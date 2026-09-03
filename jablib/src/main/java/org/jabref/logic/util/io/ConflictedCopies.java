package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Finds the copies a file synchronization client leaves next to a library when both sides changed it between two
/// syncs. Each client has its own naming scheme:
///
/// - Dropbox: `library (Alice's conflicted copy 2026-09-03).bib`
/// - Nextcloud and ownCloud: `library (conflicted copy 2026-09-03 143015).bib`
/// - Syncthing: `library.sync-conflict-20260903-143015-ABCDEFG.bib`
/// - OneDrive: `library-DESKTOP-AB12CD.bib` (the computer name; only the local computer's name is recognized, since
///   any other suffix could be an unrelated library)
///
/// Google Drive's `library (1).bib` is not recognized, because that name is too common for ordinary copies.
@NullMarked
public final class ConflictedCopies {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConflictedCopies.class);

    private ConflictedCopies() {
    }

    /// @return the conflicted copies of the given library in its directory, sorted by name (which orders Dropbox,
    /// Nextcloud, and Syncthing copies by creation time)
    public static List<Path> find(Path library) {
        Path directory = library.toAbsolutePath().getParent();
        if (directory == null || !Files.isDirectory(directory)) {
            return List.of();
        }
        Pattern pattern = patternFor(library);
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(file -> pattern.matcher(file.getFileName().toString()).matches())
                        .sorted()
                        .toList();
        } catch (IOException e) {
            LOGGER.warn("Could not look for conflicted copies of {}", library, e);
            return List.of();
        }
    }

    public static boolean isConflictedCopy(Path library, Path candidate) {
        return patternFor(library).matcher(candidate.getFileName().toString()).matches();
    }

    private static Pattern patternFor(Path library) {
        String fileName = library.getFileName().toString();
        String extension = FileUtil.getFileExtension(fileName).map(ext -> "." + ext).orElse("");
        String baseName = Pattern.quote(FileUtil.getBaseName(fileName));
        String quotedExtension = Pattern.quote(extension);
        String alternatives = baseName + " \\(.*conflicted copy.*\\)" + quotedExtension
                + "|" + baseName + "\\.sync-conflict-\\d{8}-\\d{6}-[A-Z0-9]+" + quotedExtension;
        Optional<String> computerName = localComputerName();
        if (computerName.isPresent()) {
            alternatives += "|" + baseName + "-" + Pattern.quote(computerName.get()) + "(-\\d+)?" + quotedExtension;
        }
        return Pattern.compile(alternatives, Pattern.CASE_INSENSITIVE);
    }

    /// OneDrive names conflicted copies after the computer they were made on. The name is taken from the environment
    /// to avoid the DNS lookup a host name resolution would trigger.
    static Optional<String> localComputerName() {
        return Optional.ofNullable(System.getenv("COMPUTERNAME"))
                       .or(() -> Optional.ofNullable(System.getenv("HOSTNAME")))
                       .filter(name -> !name.isBlank());
    }
}
