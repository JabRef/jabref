package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
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
            List<LinkedFile> linkedFiles = new ArrayList<>();

            try {
                linkedFiles = findAssociatedNotLinkedFiles(entry);
            } catch (IOException e) {
                result.addFileException(e);
                LOGGER.error("Problem finding files", e);
            }

            for (LinkedFile linkedFile : linkedFiles) {
                // store undo information
                onAddLinkedFile.accept(linkedFile, entry);
            }

            result.addBibEntry(entry);
        }
        return result;
    }

    /// Scans for missing files which should be linked to the given entry.
    ///
    /// Related: {@link org.jabref.gui.externalfiles.UnlinkedFilesCrawler} for scanning files missing at all entries
    ///
    /// NOTE: This method does not check if the file is already linked to another entry.
    public List<LinkedFile> findAssociatedNotLinkedFiles(BibEntry entry) throws IOException {
        List<LinkedFile> linkedFiles = new ArrayList<>();

        List<String> extensions = externalApplicationsPreferences.getExternalFileTypes().stream().map(ExternalFileType::getExtension).toList();

        LOGGER.debug("Searching for extensions {} in directories {}", extensions, directories);

        // Run the search operation
        FileFinder fileFinder = FileFinders.constructFromConfiguration(autoLinkPreferences);
        List<Path> result = new ArrayList<>(fileFinder.findAssociatedFiles(entry, directories, extensions));
        result.addAll(findByBrokenLinkName(entry));

        // Collect the found files that are not yet linked
        for (Path foundFile : result) {
            boolean fileAlreadyLinked = entry.getFiles().stream()
                                             .map(file -> file.findIn(directories))
                                             .anyMatch(linked -> linked.filter(path -> {
                                                 try {
                                                     return Files.isSameFile(path, foundFile);
                                                 } catch (IOException e) {
                                                     LOGGER.debug("Unable to check file identity, assuming no identity", e);
                                                     return false;
                                                 }
                                             }).isPresent());

            if (!fileAlreadyLinked) {
                Optional<ExternalFileType> type = FileUtil.getFileExtension(foundFile)
                                                          .map(extension -> ExternalFileTypes.getExternalFileTypeByExt(extension, externalApplicationsPreferences))
                                                          .orElse(Optional.of(new UnknownExternalFileType("")));

                String strType = type.map(ExternalFileType::getName).orElse("");
                Path relativeFilePath = FileUtil.relativize(foundFile, directories);
                LinkedFile linkedFile = LinkedFile.of("", relativeFilePath, strType);
                linkedFiles.add(linkedFile);
                LOGGER.debug("Found file {} for entry {}", linkedFile, entry.getCitationKey());
            }
        }

        return linkedFiles;
    }

    private List<Path> findByBrokenLinkName(BibEntry entry) throws IOException {
        List<Path> matches = new ArrayList<>();

        for (LinkedFile brokenLink : entry.getFiles()) {
            if (brokenLink.findIn(directories).isPresent()) {
                continue;
            }

            String wantedBase = FileUtil.getBaseName(brokenLink.getLink());

            for (Path directory : directories) {
                try (Stream<Path> walk = Files.walk(directory)) {
                    walk.filter(path -> !Files.isDirectory(path))
                        .filter(path -> FileUtil.getBaseName(path).equalsIgnoreCase(wantedBase))
                        .findFirst()
                        .ifPresent(matches::add);
                }
            }
        }

        return matches;
    }
}
