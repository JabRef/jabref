package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.FilePreferences;
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
    }

    public LinkFilesResult linkAssociatedFiles(List<BibEntry> entries, BiConsumer<LinkedFile, BibEntry> onAddLinkedFile) {
        LinkFilesResult result = new LinkFilesResult();

        for (BibEntry entry : entries) {
            Collection<LinkedFile> associatedNotLinkedFiles = new ArrayList<>();

            try {
                associatedNotLinkedFiles = findAssociatedNotLinkedFiles(entry);
            } catch (IOException e) {
                result.addFileException(e);
                LOGGER.error("Problem finding files", e);
            }

            for (LinkedFile associateNotLinkedFile : associatedNotLinkedFiles) {
                // store undo information
                onAddLinkedFile.accept(associateNotLinkedFile, entry);
            }

            result.addBibEntry(entry);
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
        List<LinkedFile> associatedNotLinkedFiles = new ArrayList<>();

        List<String> extensions = externalApplicationsPreferences.getExternalFileTypes().stream().map(ExternalFileType::getExtension).toList();

        LOGGER.debug("Searching for extensions {} in directories {}", extensions, directories);

        // Run the search operation
        FileFinder fileFinder = FileFinders.constructFromConfiguration(autoLinkPreferences);
        List<Path> result = new ArrayList<>(fileFinder.findAssociatedFiles(entry, directories, extensions));
        result.addAll(findAssociatedFilesByBrokenLinkedFile(entry));

        // Collect the found files that are not yet linked
        List<Path> linkedFiles = entry.getFiles().stream()
                                      .map(file -> file.findIn(directories))
                                      .filter(Optional::isPresent)
                                      .map(Optional::get)
                                      .toList();
        for (Path foundFile : result) {
            boolean fileAlreadyLinked = linkedFiles.stream()
                                                   .anyMatch(linked -> {
                                                       try {
                                                           return Files.isSameFile(linked, foundFile);
                                                       } catch (IOException e) {
                                                           LOGGER.debug("Unable to check file identity, assuming no identity", e);
                                                           return false;
                                                       }
                                                   });

            if (!fileAlreadyLinked) {
                Optional<ExternalFileType> type = FileUtil.getFileExtension(foundFile)
                                                          .map(extension -> ExternalFileTypes.getExternalFileTypeByExt(extension, externalApplicationsPreferences))
                                                          .orElse(Optional.of(new UnknownExternalFileType("")));

                String strType = type.map(ExternalFileType::getName).orElse("");
                Path relativeFilePath = FileUtil.relativize(foundFile, directories);
                LinkedFile linkedFile = new LinkedFile("", relativeFilePath, strType);
                associatedNotLinkedFiles.add(linkedFile);
                LOGGER.debug("Found file {} for entry {}", linkedFile, entry.getCitationKey());
            }
        }

        return associatedNotLinkedFiles;
    }

    private List<Path> findAssociatedFilesByBrokenLinkedFile(BibEntry entry) throws IOException {

        Set<String> brokenLinkBaseNames = entry.getFiles().stream()
                                               .filter(linkedFile -> linkedFile.findIn(directories).isEmpty())
                                               .map(linkedFile -> FileUtil.getBaseName(linkedFile.getLink()).toLowerCase())
                                               .collect(Collectors.toSet());

        if (brokenLinkBaseNames.isEmpty()) {
            return List.of();
        }

        List<Path> matches = new ArrayList<>();
        for (Path directory : directories) {
            try (Stream<Path> walk = Files.walk(directory)) {
                List<Path> found = walk.filter(path -> !Files.isDirectory(path))
                                       .filter(path -> brokenLinkBaseNames.contains(FileUtil.getBaseName(path).toLowerCase()))
                                       .toList();
                matches.addAll(found);
            }
        }
        return matches;
    }
}
