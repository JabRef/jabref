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
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileFinder;
import org.jabref.logic.util.io.FileFinders;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jetbrains.annotations.NotNull;
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
        this(databaseContext.getFileDirectories(filePreferences), externalApplicationsPreferences, autoLinkPreferences);
    }

    private AutoSetFileLinksUtil(List<Path> directories, ExternalApplicationsPreferences externalApplicationsPreferences, AutoLinkPreferences autoLinkPreferences) {
        this.directories = directories;
        this.autoLinkPreferences = autoLinkPreferences;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.preConfiguredFileFinder = FileFinders.constructFromConfiguration(autoLinkPreferences);
        this.brokenLinkedFileNameBasedFileFinder = FileFinders.constructBrokenLinkedFileNameBasedFileFinder();
    }

    public LinkFilesResult linkAssociatedFiles(List<BibEntry> entries, BiConsumer<LinkedFile, BibEntry> onAddLinkedFile) {
        LinkFilesResult result = new LinkFilesResult();

        for (BibEntry entry : entries) {
            doLinkAssociateFiles(entry, onAddLinkedFile, result);
        }
        return result;
    }

    /**
     * Source of associated not linked files:
     * Part A. match file name with CitationKey, (START, EXACT, REGEX) configured by user
     * Part B. match file name with broken linked file names, currently silently happen
     *
     * The auto-link process:
     * Prolog: we only consider the file a unique name
     *         if a file's name is found multiple times in Part A, we do not consider it in Step 1
     *         if a file's name is found multiple times in Part B, we do not consider it in Step 2
     * Step 1. try to auto link each broken linked file with Part A at first. For unlinked files left in Part A, add them
     *         why `add`: Part A are found mainly by CitationKey, which has strong connection to the entry, so we are
     *                    confident that we should add them automatically
     * Step 2. try to auto link each broken linked file with Part B.
     *         how about `unlinked files left in Part B`: there should be no files left as they are found based on broken
     *                                                    linked files and are used to fix them. `files left` hints a bug
     *         one trick: we accumulate Part B after Step 1, so Part A does not affect those broken linked files we use
     *                    to query Part B
     */
    private void doLinkAssociateFiles(BibEntry entry, BiConsumer<LinkedFile, BibEntry> onAddLinkedFile, LinkFilesResult result) {
        // Step 1: try matched files based on CitationKey configured by user
        Map<String, LinkedFile> files = getAssociatedFiles(entry, result, preConfiguredFileFinder);
        autoLinkBrokenLinkedFiles(entry, files);
        // Add left unlinked files as new linked files
        files.forEach((name, file) -> {
            onAddLinkedFile.accept(file, entry);
        });

        // Step 2: try matched files based on broken linked file names
        files = getAssociatedFiles(entry, result, brokenLinkedFileNameBasedFileFinder);
        autoLinkBrokenLinkedFiles(entry, files);
        // It is a bug if there are files left
        if (!files.isEmpty()) {
            LOGGER.error("Cannot auto-link all the files found based on broken linked file names. Files left: {} ",
                    files.keySet().stream().map(files::get).map(LinkedFile::getLink).collect(Collectors.joining("||")));
        }

        result.addBibEntry(entry);
    }

    private void autoLinkBrokenLinkedFiles(BibEntry entry, Map<String, LinkedFile> files) {
        for (LinkedFile linkedFile : entry.getFiles()) {
            if (isBrokenLinkedFile(linkedFile)) {
                String fileName = FileUtil.getBaseName(linkedFile.getLink());
                if (files.containsKey(fileName)) {
                    linkedFile.setLink(files.get(fileName).getLink());
                    files.remove(fileName);
                }
            }
        }
    }

    private @NotNull Map<String, LinkedFile> getAssociatedFiles(BibEntry entry, LinkFilesResult result, FileFinder finder) {
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

    private Map<String, LinkedFile> findAssociatedNotLinkedFilesWithUniqueName(BibEntry entry, FileFinder finder) throws IOException {
        Collection<LinkedFile> files = findAssociatedNotLinkedFilesWithFinder(entry, finder, getConfiguredExtensions());

        Set<String> toBeRemoved = new HashSet<>();
        Map<String, LinkedFile> result = new HashMap<>();

        files.forEach(file -> {
            String fileName = FileUtil.getBaseName(file.getLink());
            if (!result.containsKey(fileName)) {
                result.put(fileName, file);
            } else {
                toBeRemoved.add(fileName);
            }
        });

        toBeRemoved.forEach(result::remove);
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
                     .toList();
    }

    public Collection<LinkedFile> findAssociatedNotLinkedFilesWithFinder(
            BibEntry entry, FileFinder finder, List<String> extensions) throws IOException {
        // Find the associated files
        List<Path> associatedFiles = finder.findAssociatedFiles(entry, directories, extensions);

        // Collect the linked files that are not broken
        List<Path> linkedFiles =
                entry.getFiles().stream()
                     .map(file -> file.findIn(directories))
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .toList();

        // Only keep associated files that are not linked
        return associatedFiles
                .stream()
                .filter(associatedFile -> !isFileAlreadyLinked(associatedFile, linkedFiles))
                .map(this::buildLinkedFileFromPath)
                .toList();
    }

    private boolean isBrokenLinkedFile(LinkedFile file) {
        return file.findIn(directories).isEmpty();
    }

    private @NotNull LinkedFile buildLinkedFileFromPath(Path associatedFile) {
        String strType = checkAndGetFileType(associatedFile);
        Path relativeFilePath = FileUtil.relativize(associatedFile, directories);
        return new LinkedFile("", relativeFilePath, strType);
    }

    private @NotNull List<String> getConfiguredExtensions() {
        return externalApplicationsPreferences
                .getExternalFileTypes()
                .stream().map(ExternalFileType::getExtension).toList();
    }

    private @NotNull String checkAndGetFileType(Path associatedFile) {
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
