package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileFinder;
import org.jabref.logic.util.io.FileFinders;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoSetFileLinksUtil {

    public static class LinkFilesResult {
        private final List<BibEntry> changedEntries = new ArrayList<>();
        private final List<IOException> fileExceptions = new ArrayList<>();

        protected void addBibEntry(BibEntry bibEntry) {
            changedEntries.add(bibEntry);
        }

        protected void addFileException(IOException exception) {
            fileExceptions.add(exception);
        }

        public List<BibEntry> getChangedEntries() {
            return changedEntries;
        }

        public List<IOException> getFileExceptions() {
            return fileExceptions;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoSetFileLinksUtil.class);

    private final List<Path> directories;
    private final AutoLinkPreferences autoLinkPreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final FileFinder preConfiguredFileFinder;
    private final FileFinder brokenLinkedFileNameBasedFileFinder;

    public AutoSetFileLinksUtil(BibDatabaseContext databaseContext,
                                ExternalApplicationsPreferences externalApplicationsPreferences,
                                FilePreferences filePreferences,
                                AutoLinkPreferences autoLinkPreferences) {

        List<Path> dirs = new ArrayList<>(databaseContext.getFileDirectories(filePreferences));

        databaseContext.getDatabasePath().ifPresent(dbPath -> {
            Path parent = dbPath.getParent();
            if (parent != null) {
                try (Stream<Path> walk = Files.walk(parent)) {
                    walk.filter(Files::isDirectory)
                        .forEach(dir -> {
                            if (!dirs.contains(dir)) {
                                dirs.add(dir);
                            }
                        });
                } catch (IOException e) {
                    LOGGER.error("Error walking directories", e);
                }
            }
        });

        this.directories = dirs;

        // FIX 1: ensure non-null preferences
        this.autoLinkPreferences =
                autoLinkPreferences != null
                ? autoLinkPreferences
                : new AutoLinkPreferences(
                        AutoLinkPreferences.CitationKeyDependency.EXACT,
                        ".*",
                        true,
                        '.'
                );

        this.externalApplicationsPreferences = externalApplicationsPreferences;

        // FIX 2: USE this.autoLinkPreferences, NOT autoLinkPreferences
        this.preConfiguredFileFinder =
                FileFinders.constructFromConfiguration(this.autoLinkPreferences);

        this.brokenLinkedFileNameBasedFileFinder =
                FileFinders.constructBrokenLinkedFileNameBasedFileFinder();

    }

    private AutoSetFileLinksUtil(List<Path> directories, ExternalApplicationsPreferences externalApplicationsPreferences, AutoLinkPreferences autoLinkPreferences) {
        this.directories = directories;
        this.autoLinkPreferences = autoLinkPreferences;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.preConfiguredFileFinder = FileFinders.constructFromConfiguration(autoLinkPreferences);
        this.brokenLinkedFileNameBasedFileFinder = FileFinders.constructBrokenLinkedFileNameBasedFileFinder();
    }

    /// [impl->req~logic.externalfiles.file-transfer.auto-link~1]
    public LinkFilesResult linkAssociatedFiles(List<BibEntry> entries, BiConsumer<List<LinkedFile>, BibEntry> onAddLinkedFile) {
        LinkFilesResult result = new LinkFilesResult();

        for (BibEntry entry : entries) {
            doLinkAssociatedFiles(entry, onAddLinkedFile, result);
        }
        return result;
    }

    /// Source of associated not linked files:
    ///   - Part A. match file name with CitationKey, (START, EXACT, REGEX) configured by user
    ///   - Part B. match file name with broken linked file names, currently silently happen
    ///
    /// The auto-link process:
    ///   - Prolog: we only consider the file with a unique name
    ///       - if a file's name is found multiple times in Part A, we do not consider it in Step 1
    ///       - if a file's name is found multiple times in Part B, we do not consider it in Step 2
    ///   - Step 1. try to auto link each broken linked file with Part A at first. For unlinked files left in Part A, if no
    ///         linked file with same name exists, add them
    ///       - why `add`: Part A are found mainly by CitationKey, which has strong connection to the entry, so we are
    ///                    confident that we should add them automatically
    ///   - Step 2. try to auto link each broken linked file with Part B.
    ///       - how about `unlinked files left in Part B`: those files have the same name with one broken linked file
    ///                    but different extension. We skip those files.
    ///       - one trick: we accumulate Part B after Step 1, so Part A does not affect those broken linked files we use
    ///                    to query Part B
    private void doLinkAssociatedFiles(
            BibEntry entry,
            BiConsumer<List<LinkedFile>, BibEntry> onAddLinkedFile,
            LinkFilesResult result) {

        Map<String, LinkedFile> foundFiles =
                getAssociatedFiles(entry, result, preConfiguredFileFinder);

        if (foundFiles.isEmpty()) {
            return;
        }

        List<LinkedFile> currentFiles = new ArrayList<>(entry.getFiles());

        boolean updated = false;

        List<LinkedFile> updatedFiles = new ArrayList<>();

        for (LinkedFile existing : currentFiles) {

            Optional<LinkedFile> replacement =
                    foundFiles.values().stream()
                              .filter(newFile ->
                                      FileUtil.getBaseName(newFile.getLink())
                                              .equals(FileUtil.getBaseName(existing.getLink())))
                              .findFirst();

            if (replacement.isPresent()) {

                LinkedFile newFile = replacement.get();

                // ONLY replace if existing file is broken
                if (isBrokenLinkedFile(existing)) {
                    existing.setLink(newFile.getLink());
                    existing.setFileType(newFile.getFileType());
                    updated = true;
                }

                updatedFiles.add(existing);

            } else {

                updatedFiles.add(existing);

            }
        }

        // add new files not already present
        for (LinkedFile newFile : foundFiles.values()) {

            boolean exists =
                    updatedFiles.stream()
                                .anyMatch(existing ->
                                        existing.getLink().equals(newFile.getLink()));

            if (!exists) {

                updatedFiles.add(newFile);

                updated = true;
            }
        }

        if (updated) {

            onAddLinkedFile.accept(updatedFiles, entry);

            result.addBibEntry(entry);

        } else {

        }
    }

    private boolean filesChanged(List<LinkedFile> newLinkedFiles, List<LinkedFile> files) {
        String newValue = FileFieldWriter.getStringRepresentation(newLinkedFiles);
        String oldValue = FileFieldWriter.getStringRepresentation(files);

        return !Objects.equals(newValue, oldValue);
    }

    private List<LinkedFile> autoLinkBrokenLinkedFiles(List<LinkedFile> linkedFiles, Map<String, LinkedFile> files) {
        List<LinkedFile> updated = new ArrayList<>();

        for (LinkedFile linkedFile : linkedFiles) {
            String fileName = FileUtil.getBaseName(linkedFile.getLink());
            Optional<String> extension = FileUtil.getFileExtension(linkedFile.getLink());

            if (isBrokenLinkedFile(linkedFile)
                    && files.containsKey(fileName)
                    && extension.isPresent()
                    && extension.get().equalsIgnoreCase(files.get(fileName).getFileType())) {

                // FIX: update existing object, DO NOT replace
                linkedFile.setLink(files.get(fileName).getLink());

                updated.add(linkedFile);
                files.remove(fileName);

            } else {
                updated.add(linkedFile);
            }
        }

        return updated;
    }


    private Map<String, LinkedFile> getAssociatedFiles(BibEntry entry, LinkFilesResult result, FileFinder finder) {
        Map<String, LinkedFile> files;
        try {
            files = findAssociatedNotLinkedFilesWithUniqueName(entry, finder);
        } catch (IOException e) {
            result.addFileException(e);
            LOGGER.error("Problem finding files", e);
            files = Map.of();
        }
        return files;
    }

    private Map<String, LinkedFile> findAssociatedNotLinkedFilesWithUniqueName(
            BibEntry entry,
            FileFinder finder) throws IOException {

        Collection<LinkedFile> files =
                findAssociatedNotLinkedFilesWithFinder(entry, finder, getConfiguredExtensions());

        Map<String, LinkedFile> result = new HashMap<>();

        for (LinkedFile file : files) {

            String fileName = FileUtil.getBaseName(file.getLink());

            result.putIfAbsent(fileName, file);
        }

        return result;
    }

    /// Scans for missing files which should be linked to the given entry.
    ///
    /// Related: {@link org.jabref.gui.externalfiles.UnlinkedFilesCrawler} for scanning files missing at all entries
    ///
    /// NOTE:
    /// 1. This method does not check if the file is already linked to another entry.
    /// 2. This method does not guarantee how the returned files are ordered. Order by how they appear in BibEntry does
    ///    not work since findAssociatedFilesByBrokenLinkedFile may return multiple files (with the same name) for one
    ///    broken linked file in the entry.
    public Collection<LinkedFile> findAssociatedNotLinkedFiles(BibEntry entry) throws IOException {
        // Find the associated files
        List<String> extensions = getConfiguredExtensions();
        LOGGER.debug("Searching for associated not linked files with extensions {} in directories {}", extensions, directories);
        return Stream.concat(
                             findAssociatedNotLinkedFilesWithFinder(entry, preConfiguredFileFinder, extensions).stream(),
                             findAssociatedNotLinkedFilesWithFinder(entry, brokenLinkedFileNameBasedFileFinder, extensions).stream())
                     .distinct()
                     .toList();
    }

    public Collection<LinkedFile> findAssociatedNotLinkedFilesWithFinder(
            BibEntry entry, FileFinder finder, List<String> extensions) throws IOException {

        List<LinkedFile> result = new ArrayList<>();

        Optional<String> citationKeyOpt = entry.getCitationKey();

        if (citationKeyOpt.isEmpty()) {
            return result;
        }

        String citationKey = citationKeyOpt.get();

        for (Path dir : directories) {

            try (Stream<Path> walk = Files.walk(dir)) {

                walk.filter(Files::isRegularFile)
                    .forEach(path -> {

                        Optional<String> ext = FileUtil.getFileExtension(path);

                        if (ext.isEmpty()) return;

                        if (!extensions.contains(ext.get())) return;

                        String fileName = FileUtil.getBaseName(path);

                        if (citationKey.equals(fileName)) {
                            result.add(buildLinkedFileFromPath(path));
                        }
                    });

            }
        }

        return result;
    }

    private boolean isBrokenLinkedFile(LinkedFile file) {
        return file.findIn(directories).isEmpty();
    }

    private LinkedFile buildLinkedFileFromPath(Path associatedFile) {

        String fileType = checkAndGetFileType(associatedFile);

        Path relativePath = associatedFile;

        try {
            Optional<Path> matchingDirectory = directories.stream()
                                                          .filter(dir -> associatedFile.startsWith(dir))
                                                          .findFirst();

            if (matchingDirectory.isPresent()) {
                relativePath = matchingDirectory.get().relativize(associatedFile);
            }

        } catch (IllegalArgumentException e) {
            LOGGER.debug("Could not relativize path {}", associatedFile, e);
        }

        return new LinkedFile(
                "",
                relativePath,
                fileType
        );
    }

    private List<String> getConfiguredExtensions() {
        return externalApplicationsPreferences
                .getExternalFileTypes()
                .stream().map(ExternalFileType::getExtension).toList();
    }

    private String checkAndGetFileType(Path associatedFile) {
        return FileUtil.getFileExtension(associatedFile)
                       .flatMap(extension ->
                               ExternalFileTypes.getExternalFileTypeByExt(
                                       extension,
                                       externalApplicationsPreferences
                               ))
                       .map(ExternalFileType::getName).orElse("");
    }

    private static boolean isFileAlreadyLinked(Path foundFile, List<Path> linkedFiles) {
        return linkedFiles.stream()
                          .anyMatch(linked -> {
                              try {
                                  return Files.isSameFile(linked, foundFile);
                              } catch (IOException e) {
                                  LOGGER.debug("Unable to check file identity, assuming no identity", e);
                                  return false;
                              }
                          });
    }
}
